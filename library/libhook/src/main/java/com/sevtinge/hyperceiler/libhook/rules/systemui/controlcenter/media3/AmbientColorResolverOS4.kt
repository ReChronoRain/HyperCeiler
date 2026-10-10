/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Looper
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.ceil
import kotlin.math.sqrt

/** One owner's stateless resolver. Call resolve only from its background task. */
internal class AmbientColorResolverOS4 private constructor(
    val mediaDataClass: Class<*>,
    private val artwork: Field,
    private val acquireIcon: Method,
    private val standard: Standard?,
    private val optimized: Optimized?
) {
    data class Colors(val light: Int, val dark: Int)

    private class Standard(
        val drawableToBitmap: Method,
        val getPixels: Method,
        val maxArea: Field
    )

    private class Optimized(
        val constructor: Constructor<*>,
        val accent: Field,
        val shades: Field
    )

    /** Null is unavailable; never substitutes transparent for a failed operation. */
    fun resolve(context: Context, mediaData: Any, optimized: Boolean): Colors? {
        if (Looper.myLooper() === Looper.getMainLooper() || Thread.currentThread().isInterrupted) return null
        if (mediaData.javaClass !== mediaDataClass) return null
        if (optimized) {
            if (this.optimized == null) return null
        } else if (standard == null) return null
        return recover {
            val icon = artwork.get(mediaData) as? Icon
            val drawable = recover { icon?.loadDrawable(context)?.let { privateDrawable(context, it) } }
                ?: recover { (acquireIcon.invoke(null, context, mediaData) as? Drawable)?.let { privateDrawable(context, it) } }
                ?: return null
            if (Thread.currentThread().isInterrupted) return null
            if (optimized) optimizedColors(drawable) else standardColors(drawable)
        }
    }

    private fun privateDrawable(context: Context, original: Drawable, depth: Int = 0): Drawable? {
        if (depth > 8) return null
        val name = original.javaClass.name
        val quietGetter = when (name) {
            "com.miui.maml.FancyDrawable", "com.miui.maml.AnimatingDrawable" -> "getQuietDrawable"
            "com.miui.maml.MamlAdaptiveIconDrawable" -> "getTopQuietDrawable"
            else -> null
        }
        if (quietGetter != null) {
            // These exact OTA getters only read the already-created quietImage.
            // Never clone Fancy: its ConstantState registers with the same RendererCore.
            val getter = original.javaClass.getDeclaredMethod(quietGetter)
            if (getter.returnType !== Drawable::class.java || Modifier.isStatic(getter.modifiers) ||
                !Modifier.isPublic(getter.modifiers)) return null
            val quiet = getter.invoke(original) as? Drawable
            if (quiet != null && quiet !== original) {
                privateDrawable(context, quiet, depth + 1)?.let { return it }
            }
            if (name != "com.miui.maml.MamlAdaptiveIconDrawable") return null
            // No top quiet image: preserve the adaptive structure using private
            // static layer snapshots. No prepareFancyDrawable/getRoot/tick is used.
            return privateAdaptive(context, original as? AdaptiveIconDrawable ?: return null, depth)
        }
        var ancestor: Class<*>? = original.javaClass
        while (ancestor != null) {
            if (ancestor.name.startsWith("com.miui.maml.")) return null
            ancestor = ancestor.superclass
        }
        if (original.javaClass === AdaptiveIconDrawable::class.java) return privateAdaptive(context, original as AdaptiveIconDrawable, depth)
        if (original is BitmapDrawable) {
            val bitmap = original.bitmap ?: return null
            if (bitmap.isRecycled) return null
            val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
            if (copy === bitmap || copy.isRecycled || copy.config == Bitmap.Config.HARDWARE) return null
            return BitmapDrawable(context.resources, copy)
        }
        // Restrict generic ConstantState cloning to framework leaf drawables. A
        // custom wrapper can otherwise hide an animation root in its state.
        if (original.javaClass.classLoader !== Drawable::class.java.classLoader || name !in setOf(
                "android.graphics.drawable.Drawable", "android.graphics.drawable.ColorDrawable",
                "android.graphics.drawable.GradientDrawable", "android.graphics.drawable.VectorDrawable",
                "android.graphics.drawable.NinePatchDrawable", "android.graphics.drawable.ShapeDrawable"
            )) return null
        val created = original.constantState?.newDrawable(context.resources) ?: return null
        if (created === original) return null
        val private = created.mutate()
        if (private === original) return null
        // A factory is allowed to return a BitmapDrawable; its underlying bitmap
        // still needs a copy because ConstantState may share pixel storage.
        return if (private is BitmapDrawable) privateDrawable(context, private, depth + 1) else private
    }

    private fun privateAdaptive(context: Context, original: AdaptiveIconDrawable, depth: Int): Drawable? {
        val background = original.background
        val foreground = original.foreground
        if (background == null && foreground == null) return null
        val privateBackground = background?.let { privateDrawable(context, it, depth + 1) ?: return null }
        val privateForeground = foreground?.let { privateDrawable(context, it, depth + 1) ?: return null }
        return AdaptiveIconDrawable(privateBackground, privateForeground)
    }

    private fun optimizedColors(drawable: Drawable): Colors? {
        val api = optimized ?: return null
        val colors = if (drawable is BitmapDrawable) {
            val bitmap = readableBitmap(drawable.bitmap) ?: return null
            WallpaperColors.fromBitmap(bitmap)
        } else {
            val bounds = Rect(drawable.bounds)
            try {
                WallpaperColors.fromDrawable(drawable)
            } finally {
                drawable.bounds = bounds
            }
        } ?: return null
        if (Thread.currentThread().isInterrupted) return null
        // OS4's integer CONTENT style is 6. Keep the original dark-scheme palette.
        val scheme = api.constructor.newInstance(colors, true, 6)
        val accent = api.accent.get(scheme) ?: return null
        val shades = api.shades.get(accent) as? List<*> ?: return null
        if (shades.size != 13 || shades.any { it !is Int }) return null
        return Colors(shades[4] as Int, shades[7] as Int)
    }

    private fun standardColors(drawable: Drawable): Colors? {
        val api = standard ?: return null
        val bounds = Rect(drawable.bounds)
        val converted = try {
            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                // The OTA converter rejects dimensionless drawables before reading
                // requested sizes. Render our private static snapshot at MAX_AREA.
                Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888).also {
                    drawable.setBounds(0, 0, 112, 112)
                    drawable.draw(Canvas(it))
                }
            } else api.drawableToBitmap.invoke(null, drawable, 0, 0) as? Bitmap
        } finally {
            drawable.bounds = bounds
        }
        val bitmap = readableBitmap(converted) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val area = width.toLong() * height.toLong()
        if (width <= 0 || height <= 0 || area > Int.MAX_VALUE) return null
        // This OTA assigns MAX_AREA only in clinit: 0x3100; its buffer is twice it.
        // Reject changed/unknown bounds before native code or a potentially huge scale.
        val maxArea = api.maxArea.getInt(null)
        if (maxArea != 0x3100) return null
        val capacity = maxArea * 2
        val scale = if (area > maxArea) sqrt((maxArea.toFloat() / area.toFloat()).toDouble()).toFloat() else 1f
        val expectedWidth = if (area > maxArea) ceil((width.toFloat() * scale).toDouble()).toInt() else width
        val expectedHeight = if (area > maxArea) ceil((height.toFloat() * scale).toDouble()).toInt() else height
        val expectedArea = expectedWidth.toLong() * expectedHeight.toLong()
        if (expectedWidth <= 0 || expectedHeight <= 0 || expectedArea > capacity) return null
        // Original ROM resampler with a task-owned array. The dedicated native
        // bridge also isolates the vendor algorithm's mutable native instance.
        val pixels = IntArray(capacity)
        val sampled = api.getPixels.invoke(null, pixels, bitmap, null) as? Bitmap ?: return null
        if (sampled.isRecycled || sampled.width != expectedWidth || sampled.height != expectedHeight) return null
        if (api.maxArea.getInt(null) != maxArea || Thread.currentThread().isInterrupted) return null
        val color = AmbientNativeColorOS4.resolve(pixels, sampled.width, sampled.height) ?: return null
        return Colors(color, color)
    }

    private fun readableBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null || bitmap.isRecycled) return null
        // The input may be a borrowed artwork bitmap. Never recycle it or any
        // bitmap returned by vendor helpers; software copies have ordinary GC lifetime.
        return if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
    }

    companion object {
        /** No class initialization or unselected mode lookup is performed here. */
        fun create(loader: ClassLoader, needsStandard: Boolean, needsOptimized: Boolean): AmbientColorResolverOS4? {
            return recover {
                if (!needsStandard && !needsOptimized) return null
                fun type(name: String) = Class.forName(name, false, loader)
                val media = type("com.android.systemui.media.controls.shared.model.MediaData")
                val icon = media.getDeclaredField("artwork").also {
                    require(it.type === Icon::class.java && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers))
                    it.isAccessible = true
                }
                val utility = type("com.android.systemui.statusbar.notification.utils.NotificationUtil")
                val acquire = method(utility, "acquireApplicationIcon", Drawable::class.java, Context::class.java, media)
                val normal = if (needsStandard) {
                    val utils = type("com.miui.utils.DrawableUtils")
                    val palette = type("miuix.mipalette.MiPalette")
                    val pixels = palette.getDeclaredMethod("getPixels", IntArray::class.java, Bitmap::class.java, List::class.java).also {
                        require(it.returnType === Bitmap::class.java && Modifier.isStatic(it.modifiers) && Modifier.isPrivate(it.modifiers) && !Modifier.isNative(it.modifiers))
                        it.isAccessible = true
                    }
                    method(palette, "getMainColorHCT", Int::class.javaPrimitiveType!!, IntArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!).also {
                        require(Modifier.isNative(it.modifiers))
                    }
                    val maxArea = palette.getDeclaredField("MAX_AREA").also {
                        require(it.type === Int::class.javaPrimitiveType && Modifier.isStatic(it.modifiers))
                        it.isAccessible = true
                    }
                    Standard(method(utils, "drawable2Bitmap", Bitmap::class.java, Drawable::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!), pixels, maxArea)
                } else null
                val opt = if (needsOptimized) {
                    val scheme = type("com.android.systemui.monet.ColorScheme")
                    val tonal = type("com.android.systemui.monet.TonalPalette")
                    val constructor = scheme.getDeclaredConstructor(WallpaperColors::class.java, Boolean::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!).also {
                        require(Modifier.isPublic(it.modifiers))
                    }
                    val accent = scheme.getDeclaredField("mAccent1").also {
                        require(it.type === tonal && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers))
                        it.isAccessible = true
                    }
                    val shades = tonal.getDeclaredField("allShades").also {
                        require(it.type === List::class.java && !Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers))
                        it.isAccessible = true
                    }
                    Optimized(constructor, accent, shades)
                } else null
                AmbientColorResolverOS4(media, icon, acquire, normal, opt)
            }
        }

        private fun method(owner: Class<*>, name: String, result: Class<*>, vararg args: Class<*>): Method =
            owner.getDeclaredMethod(name, *args).also {
                require(it.returnType === result && Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers))
                it.isAccessible = true
            }

        private inline fun <T> recover(block: () -> T?): T? = try {
            block()
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
    }
}
