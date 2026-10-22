/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Looper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Stateless, task-local rendering. No preferences, holders, or global image cache. */
// Keep this owner-specific lifecycle and its private rollback helpers in one auditable scope.
// This immutable contract records the complete validated ROM shape or captured invocation.
@Suppress("LongParameterList", "TooManyFunctions")
internal class CustomBackgroundRendererOS4 private constructor(
    val mediaDataClass: Class<*>,
    private val artwork: Field,
    private val acquireIcon: Method,
    private val constructor: Constructor<*>,
    private val palettes: List<Field>,
    private val shades: Field,
    private val selectedStyles: Set<Int>
) {
    data class Options(val colorAnimation: Boolean = false, val inverse: Boolean = false, val blurPercent: Int = 10)

    /** Only immutable software pixels escape the task. Each holder gets its own Drawable. */
    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    class Frame internal constructor(
        private val resources: Resources,
        private val background: Bitmap,
        val colors: MediaViewColorConfig,
        val width: Int,
        val height: Int,
        private val style: Int,
        private val colorAnimation: Boolean
    ) {
        fun newPresentation(resizeAnimation: Boolean): CustomBackgroundDrawableOS4 =
            CustomBackgroundDrawableOS4(BitmapDrawable(resources, background), colors, style, colorAnimation).also {
                it.setResizeAnim(resizeAnimation)
                it.setBounds(0, 0, width, height)
            }
        fun applyTo(presentation: CustomBackgroundDrawableOS4, animate: Boolean) {
            presentation.update(BitmapDrawable(resources, background), colors, !animate)
            presentation.setBounds(0, 0, width, height)
        }
        // No recycle/dispose: a superseded Frame may still be displayed by one
        // of the two DI holders. Ordinary Bitmap GC follows those references.
    }

    /** A null result is failure, including GPU failure; never a transparent success. */
    // This immutable contract records the complete validated ROM shape or captured invocation.
    @Suppress("LongParameterList")
    fun render(context: Context, mediaData: Any, style: Int, options: Options, width: Int, height: Int): Frame? {
        if (Looper.myLooper() === Looper.getMainLooper() || Thread.currentThread().isInterrupted) return null
        if (mediaData.javaClass !== mediaDataClass || style !in selectedStyles || options.blurPercent !in 0..20) return null
        if (width !in 0..16384 || height !in 0..16384) return null
        return recover {
            val tasks = OwnedBitmaps()
            try {
                val icon = artwork.get(mediaData) as? Icon
                val image = recover { icon?.loadDrawable(context)?.let { snapshot(context, it, tasks) } }
                    ?: recover { (acquireIcon.invoke(null, context, mediaData) as? Drawable)?.let { snapshot(context, it, tasks) } }
                    ?: return null
                val targetWidth = if (width > 0) width else image.width
                val targetHeight = if (height > 0) height else image.height
                val palette = palette(image) ?: return null
                val color = colorConfig(style, options.inverse, image, palette)
                if (Thread.currentThread().isInterrupted) return null
                val processed = when (style) {
                    1 -> coverArt(context, image, color, tasks)
                    2 -> blurredCover(image, color, targetHeight, options.blurPercent, tasks)
                    3 -> square(image, false, color.bgStartColor, tasks)
                    4 -> square(image, true, color.bgStartColor, tasks)
                    else -> return null
                } ?: return null
                if (Thread.currentThread().isInterrupted) return null
                // Copies cannot alias the mutable task images. Do not recycle
                // successfully published pixels, even when another request wins.
                val background = processed.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                tasks.add(background)
                if (background === processed || background.isMutable) return null
                val frame = Frame(context.resources, background, color, targetWidth, targetHeight, style, options.colorAnimation)
                tasks.release(background)
                frame
            } finally { tasks.close() }
        }
    }

    private fun palette(image: Bitmap): List<List<Int>>? {
        val wallpaper = WallpaperColors.fromBitmap(image) ?: return null
        val scheme = constructor.newInstance(wallpaper, true, 6)
        return palettes.map { field ->
            val value = field.get(scheme) ?: return null
            val entries = shades.get(value) as? List<*> ?: return null
            if (entries.size != 13 || entries.any { it !is Int }) return null
            entries.map { it as Int }
        }
    }

    private fun colorConfig(style: Int, inverse: Boolean, image: Bitmap, palette: List<List<Int>>): MediaViewColorConfig {
        val (neutral1, neutral2, accent1, accent2) = palette
        return when (style) {
            1 -> MediaViewColorConfig(accent1[2], accent1[2], accent1[8], accent1[8])
            2, 3 -> MediaViewColorConfig(neutral1[1], neutral2[3], accent2[9], accent1[9])
            4 -> if (inverse && brightness(image) >= 192f)
                MediaViewColorConfig(accent1[8], accent1[8], accent1[3], accent1[3])
            else MediaViewColorConfig(accent1[2], accent1[2], accent1[8], accent1[8])
            else -> error("Unselected style")
        }
    }

    private fun snapshot(context: Context, original: Drawable, tasks: OwnedBitmaps, depth: Int = 0): Bitmap? {
        if (depth > 8) return null
        if (original is BitmapDrawable) {
            val source = original.bitmap ?: return null
            if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
            val size = dimensions(source.width, source.height)
            if (source.config == Bitmap.Config.HARDWARE) {
                val scaled = Bitmap.createScaledBitmap(source, size.first, size.second, true)
                if (scaled !== source) tasks.add(scaled)
                val copied = scaled.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                if (copied === source || copied === scaled || copied.isRecycled) return null
                tasks.add(copied)
                return copied
            }
            val result = tasks.create(size.first, size.second)
            Canvas(result).drawBitmap(source, null, Rect(0, 0, size.first, size.second), Paint(Paint.FILTER_BITMAP_FLAG))
            return result
        }
        val name = original.javaClass.name
        val quietName = when (name) {
            "com.miui.maml.FancyDrawable", "com.miui.maml.AnimatingDrawable" -> "getQuietDrawable"
            "com.miui.maml.MamlAdaptiveIconDrawable" -> "getTopQuietDrawable"
            else -> null
        }
        if (quietName != null) {
            val getter = original.javaClass.getDeclaredMethod(quietName)
            if (getter.returnType !== Drawable::class.java || Modifier.isStatic(getter.modifiers) || !Modifier.isPublic(getter.modifiers)) return null
            val quiet = getter.invoke(original) as? Drawable
            if (quiet != null && quiet !== original) snapshot(context, quiet, tasks, depth + 1)?.let { return it }
            if (name != "com.miui.maml.MamlAdaptiveIconDrawable") return null
            return adaptive(context, original as? AdaptiveIconDrawable ?: return null, tasks, depth)
        }
        var ancestor: Class<*>? = original.javaClass
        while (ancestor != null) {
            if (ancestor.name.startsWith("com.miui.maml.")) return null
            ancestor = ancestor.superclass
        }
        if (original.javaClass === AdaptiveIconDrawable::class.java) return adaptive(context, original as AdaptiveIconDrawable, tasks, depth)
        if (original.javaClass.classLoader !== Drawable::class.java.classLoader || name !in STATIC_DRAWABLES) return null
        val copy = original.constantState?.newDrawable(context.resources) ?: return null
        if (copy === original) return null
        val private = copy.mutate()
        if (private === original) return null
        if (private is BitmapDrawable) return snapshot(context, private, tasks, depth + 1)
        return drawPrivate(private, tasks)
    }

    private fun adaptive(context: Context, source: AdaptiveIconDrawable, tasks: OwnedBitmaps, depth: Int): Bitmap? {
        val background = source.background
        val foreground = source.foreground
        if (background == null && foreground == null) return null
        val a = background?.let { snapshot(context, it, tasks, depth + 1) ?: return null }
        val b = foreground?.let { snapshot(context, it, tasks, depth + 1) ?: return null }
        return drawPrivate(AdaptiveIconDrawable(a?.let { BitmapDrawable(context.resources, it) }, b?.let { BitmapDrawable(context.resources, it) }), tasks)
    }

    private fun drawPrivate(value: Drawable, tasks: OwnedBitmaps): Bitmap {
        val size = dimensions(value.intrinsicWidth, value.intrinsicHeight)
        val result = tasks.create(size.first, size.second)
        val bounds = Rect(value.bounds)
        try { value.setBounds(0, 0, size.first, size.second); value.draw(Canvas(result)) }
        finally { value.bounds = bounds }
        return result
    }

    private fun dimensions(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 112 to 112
        val scale = min(1.0, 1024.0 / max(width, height).toDouble())
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    private fun square(image: Bitmap, fill: Boolean, color: Int, tasks: OwnedBitmaps): Bitmap {
        val size = if (fill) min(image.width, image.height) else max(image.width, image.height)
        val result = tasks.create(size, size)
        val canvas = Canvas(result)
        canvas.drawColor(color)
        canvas.drawBitmap(image, (size - image.width) / 2f, (size - image.height) / 2f, null)
        return result
    }

    private fun blurredCover(image: Bitmap, colors: MediaViewColorConfig, height: Int, blur: Int, tasks: OwnedBitmaps): Bitmap? {
        val masked = tasks.create(image.width, image.height)
        val canvas = Canvas(masked)
        canvas.drawColor(colors.bgStartColor)
        canvas.drawBitmap(image, 0f, 0f, null)
        GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            this.colors = intArrayOf((colors.bgStartColor and 0xffffff) or (64 shl 24), colors.bgEndColor or (255 shl 24))
            gradientRadius = max(image.width, image.height).toFloat()
            setBounds(0, 0, image.width, image.height)
            draw(canvas)
        }
        // Zero is an actual no-blur setting. Keep the color mask and do not
        // construct an invalid zero-radius RenderEffect or open GPU resources.
        val blurred = if (blur == 0) masked else hardwareBlur(masked, height.toFloat() / 100f * blur, tasks) ?: return null
        return square(blurred, false, Color.TRANSPARENT, tasks)
    }

    private fun coverArt(context: Context, image: Bitmap, colors: MediaViewColorConfig, tasks: OwnedBitmaps): Bitmap? {
        val result = tasks.create(264, 264)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val positions = arrayOf(0 to 0, 132 to 0, 0 to 132, 132 to 132, 99 to 99)
        positions.forEachIndexed { index, (x, y) ->
            val size = if (index == 4) 66 else 132
            val saved = canvas.save()
            try {
                canvas.translate(x.toFloat(), y.toFloat())
                canvas.rotate(Random.nextInt(4) * 90f, size / 2f, size / 2f)
                canvas.scale(if (Random.nextBoolean()) -1f else 1f, if (Random.nextBoolean()) -1f else 1f, size / 2f, size / 2f)
                canvas.drawBitmap(image, null, Rect(0, 0, size, size), paint)
            } finally { canvas.restoreToCount(saved) }
        }
        val brightness = brightness(result)
        val adjustment = when { brightness < 50 -> 40f; brightness < 100 -> 20f; brightness < 200 -> -20f; else -> -40f }
        val corrected = tasks.create(264, 264)
        val correctionCanvas = Canvas(corrected)
        paint.colorFilter = ColorMatrixColorFilter(floatArrayOf(1f, 0f, 0f, 0f, adjustment, 0f, 1f, 0f, 0f, adjustment, 0f, 0f, 1f, 0f, adjustment, 0f, 0f, 0f, 1f, 0f))
        correctionCanvas.drawBitmap(result, 0f, 0f, paint)
        correctionCanvas.drawColor(colors.bgStartColor and 0x6fffffff)
        val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val shade = if (dark) 0 else 248
        correctionCanvas.drawColor(Color.argb(20, shade, shade, shade))
        return hardwareBlur(corrected, 40f, tasks)
    }

    private fun brightness(image: Bitmap): Float {
        var sum = 0.0
        var count = 0
        for (y in 0 until image.height step 5) for (x in 0 until image.width step 5) {
            val pixel = image.getPixel(x, y)
            sum += 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            count++
        }
        return (sum / count).toFloat()
    }

    private fun hardwareBlur(image: Bitmap, radius: Float, tasks: OwnedBitmaps): Bitmap? {
        if (!radius.isFinite() || radius <= 0f || Thread.currentThread().isInterrupted) return null
        var reader: ImageReader? = null
        var renderer: HardwareRenderer? = null
        var node: RenderNode? = null
        var frame: Image? = null
        var buffer: HardwareBuffer? = null
        var wrapped: Bitmap? = null
        try {
            reader = ImageReader.newInstance(image.width, image.height, PixelFormat.RGBA_8888, 1,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
            node = RenderNode("HyperCeilerOS4MediaBlur")
            renderer = HardwareRenderer()
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            node.setPosition(0, 0, image.width, image.height)
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR))
            val canvas = node.beginRecording()
            try { canvas.drawBitmap(image, 0f, 0f, null) } finally { node.endRecording() }
            if (renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw() != HardwareRenderer.SYNC_OK) return null
            frame = reader.acquireNextImage() ?: return null
            buffer = frame.hardwareBuffer ?: return null
            wrapped = Bitmap.wrapHardwareBuffer(buffer, null) ?: return null
            val result = wrapped.copy(Bitmap.Config.ARGB_8888, false) ?: return null
            if (result === wrapped || result.config == Bitmap.Config.HARDWARE || result.isRecycled) return null
            tasks.add(result)
            return result
        } finally {
            // Each cleanup is independent: one broken driver close must not skip
            // the remaining acquired resources. No borrowed input is recycled.
            cleanup { wrapped?.recycle() }
            cleanup { buffer?.close() }
            cleanup { frame?.close() }
            cleanup { renderer?.destroy() }
            cleanup { node?.discardDisplayList() }
            cleanup { reader?.close() }
        }
    }

    private class OwnedBitmaps {
        private val values = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Bitmap, Boolean>())
        fun add(value: Bitmap): Bitmap { values.add(value); return value }
        fun create(width: Int, height: Int) = add(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
        fun release(value: Bitmap) { values.remove(value) }
        fun close() { values.forEach { cleanup { if (!it.isRecycled) it.recycle() } }; values.clear() }
    }

    companion object {
        private val STATIC_DRAWABLES = setOf("android.graphics.drawable.ColorDrawable", "android.graphics.drawable.GradientDrawable",
            "android.graphics.drawable.VectorDrawable", "android.graphics.drawable.NinePatchDrawable", "android.graphics.drawable.ShapeDrawable")

        fun create(loader: ClassLoader, selectedStyles: Set<Int>): CustomBackgroundRendererOS4? {
            return recover {
                if (selectedStyles.isEmpty() || selectedStyles.any { it !in 1..4 }) return null
                fun type(name: String) = Class.forName(name, false, loader)
                val media = type("com.android.systemui.media.controls.shared.model.MediaData")
                val art = media.getDeclaredField("artwork").also {
                    require(it.type === Icon::class.java && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers)); it.isAccessible = true
                }
                val acquire = type("com.android.systemui.statusbar.notification.utils.NotificationUtil")
                    .getDeclaredMethod("acquireApplicationIcon", Context::class.java, media).also {
                        require(it.returnType === Drawable::class.java && Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers)); it.isAccessible = true
                    }
                val scheme = type("com.android.systemui.monet.ColorScheme")
                val tonal = type("com.android.systemui.monet.TonalPalette")
                val constructor = scheme.getDeclaredConstructor(WallpaperColors::class.java, Boolean::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
                require(Modifier.isPublic(constructor.modifiers))
                val palettes = listOf("mNeutral1", "mNeutral2", "mAccent1", "mAccent2").map { name -> scheme.getDeclaredField(name).also {
                    require(it.type === tonal && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers)); it.isAccessible = true
                } }
                val shades = tonal.getDeclaredField("allShades").also {
                    require(it.type === List::class.java && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers)); it.isAccessible = true
                }
                CustomBackgroundRendererOS4(media, art, acquire, constructor, palettes, shades, selectedStyles.toSet())
            }
        }

        private inline fun cleanup(block: () -> Unit) { try { block() } catch (_: Exception) {} catch (_: LinkageError) {} }
        private inline fun <T> recover(block: () -> T?): T? = try { block() } catch (_: Exception) { null } catch (_: LinkageError) { null } catch (_: OutOfMemoryError) { null }
    }
}
