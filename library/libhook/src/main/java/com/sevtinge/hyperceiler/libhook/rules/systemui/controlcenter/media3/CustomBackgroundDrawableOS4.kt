/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import kotlin.math.max
import kotlin.math.min

/**
 * One media holder owns one instance. Artwork must be a private Drawable;
 * immutable pixels may be shared, but no borrowed pixels are ever recycled here.
 * All state and drawing operations run on the view's main thread.
 */
// Keep this owner-specific lifecycle and its private rollback helpers in one auditable scope.
@Suppress("TooManyFunctions")
class CustomBackgroundDrawableOS4(
    artwork: Drawable,
    colors: MediaViewColorConfig,
    private val style: Int,
    private val useAnim: Boolean
) : Drawable() {
    private var artwork: Drawable? = artwork
    private var incoming: Drawable? = null
    private var sourceColors = colors
    private var targetColors = colors
    private var currentBgStartColor = colors.bgStartColor
    private var currentBgEndColor = colors.bgEndColor
    private var albumStart = -1L
    private var resizeStart = -1L
    private var currentSize = 0f
    private var sourceSize = 0f
    private var targetSize = 0f
    private var resizeEnabled = false
    private var paused = false
    private var disposed = false
    private var opacity = 255
    private var filter: ColorFilter? = null
    private val layerPaint = Paint()
    private val fillPaint = Paint()
    private val gradient = GradientDrawable()
    private val imageRect = Rect()
    private val artworkBounds = Rect()
    private val radialGradientColors = IntArray(2)
    private val linearGradientColors = IntArray(2)

    init { require(style in 1..4) { "Unknown OS4 media background style" } }

    /** Controls size interpolation; disabling it snaps to the latest bounds. */
    fun setResizeAnim(enabled: Boolean) {
        resizeEnabled = enabled
        if (!enabled) { resizeStart = -1L; currentSize = targetSize }
        invalidateSelf()
    }

    /** Replaces holder-owned artwork, optionally crossfading from the current image. */
    fun update(artwork: Drawable, colors: MediaViewColorConfig, skipAnim: Boolean = false) {
        if (disposed) return
        // Bound retained images to two even when artwork updates outpace animation.
        finishAnimations()
        val animate = useAnim && !skipAnim && canAnimate()
        if (!animate) {
            this.artwork = artwork
            setColors(colors)
            sourceColors = colors
            targetColors = colors
        } else {
            incoming = artwork
            sourceColors = targetColors
            targetColors = colors
            albumStart = SystemClock.uptimeMillis()
        }
        invalidateSelf()
    }

    private fun canAnimate(): Boolean {
        if (paused || !isVisible) return false
        val host = callback as? View ?: return false
        return host.isShown && host.isAttachedToWindow
    }

    /** Snap to the newest bound result without keeping an offscreen frame loop. */
    fun finishAnimations() {
        incoming?.let { artwork = it }
        incoming = null
        if (albumStart >= 0) setColors(targetColors)
        sourceColors = targetColors
        albumStart = -1L
        currentSize = targetSize
        sourceSize = targetSize
        resizeStart = -1L
        invalidateSelf()
    }

    /** Stops frame scheduling while preserving the latest artwork. */
    fun setPaused(value: Boolean) {
        if (paused == value || disposed) return
        paused = value
        if (value) finishAnimations() else invalidateSelf()
    }

    /** Updates whether the owning media view may animate. */
    fun setActive(value: Boolean) = setPaused(!value)

    /** Releases references and stops invalidations; callbacks cannot revive it. */
    fun dispose() {
        if (disposed) return
        finishAnimations()
        disposed = true
        artwork = null
        incoming = null
        callback = null
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (!visible && !disposed) finishAnimations()
        return changed
    }

    override fun onBoundsChange(bounds: Rect) {
        val size = if (style == 4) min(bounds.width(), bounds.height()).toFloat()
            else max(bounds.width(), bounds.height()).toFloat()
        val next = size.coerceAtLeast(0f)
        if (targetSize == next) return
        targetSize = next
        val resizeAllowed = useAnim && resizeEnabled && !paused
        val hasVisibleSize = isVisible && currentSize > 0f && next > 0f
        if (resizeAllowed && hasVisibleSize) {
            sourceSize = currentSize
            resizeStart = SystemClock.uptimeMillis()
        } else {
            currentSize = next
            sourceSize = next
            resizeStart = -1L
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (disposed || !isVisible) return
        if (bounds.isEmpty || artwork == null) return
        val now = SystemClock.uptimeMillis()
        val progress = advanceAnimations(now)
        val outer = canvas.save()
        try {
            canvas.clipRect(bounds)
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()
            // Alpha and filter cover artwork, solid fill, and gradient together.
            if (opacity == 255 && filter == null) {
                drawLayer(canvas, width, height, progress)
            } else {
                layerPaint.alpha = opacity
                layerPaint.colorFilter = filter
                val layer = canvas.saveLayer(0f, 0f, width, height, layerPaint)
                try {
                    drawLayer(canvas, width, height, progress)
                } finally { canvas.restoreToCount(layer) }
            }
        } finally { canvas.restoreToCount(outer) }
        if (!paused && (albumStart >= 0 || resizeStart >= 0)) invalidateSelf()
    }

    private fun drawLayer(canvas: Canvas, width: Float, height: Float, progress: Float) {
        fillPaint.color = currentBgStartColor
        canvas.drawRect(0f, 0f, width, height, fillPaint)
        val size = currentSize.toInt().coerceAtLeast(1)
        if (style == 4) imageRect.set(bounds.width() - size, 0, bounds.width(), size)
        else {
            val left = (bounds.width() - size) / 2
            val top = (bounds.height() - size) / 2
            imageRect.set(left, top, left + size, top + size)
        }
        drawArtwork(canvas, checkNotNull(artwork))
        incoming?.let {
            val cover = canvas.saveLayerAlpha(0f, 0f, width, height, (progress * 255).toInt())
            try { drawArtwork(canvas, it) } finally { canvas.restoreToCount(cover) }
        }
        if (style == 3) {
            gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
            gradient.gradientRadius = size.toFloat()
            radialGradientColors[0] = withAlpha(currentBgStartColor, 64)
            radialGradientColors[1] = withAlpha(currentBgEndColor, 255)
            gradient.colors = radialGradientColors
            gradient.bounds = imageRect
            gradient.draw(canvas)
        } else if (style == 4) {
            gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
            gradient.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            linearGradientColors[0] = currentBgStartColor
            linearGradientColors[1] = withAlpha(currentBgStartColor, 51)
            gradient.colors = linearGradientColors
            gradient.setBounds(imageRect.left, 0, bounds.width(), bounds.height())
            gradient.draw(canvas)
        }
    }

    private fun advanceAnimations(now: Long): Float {
        var progress = 0f
        if (albumStart >= 0) {
            progress = ((now - albumStart) / 333f).coerceIn(0f, 1f)
            currentBgStartColor = blend(sourceColors.bgStartColor, targetColors.bgStartColor, progress)
            currentBgEndColor = blend(sourceColors.bgEndColor, targetColors.bgEndColor, progress)
            if (progress >= 1f) {
                artwork = incoming ?: artwork
                incoming = null
                albumStart = -1L
                setColors(targetColors)
                progress = 0f
            }
        }
        if (resizeStart >= 0) {
            val fraction = ((now - resizeStart) / 234f).coerceIn(0f, 1f)
            currentSize = sourceSize + (targetSize - sourceSize) * fraction
            if (fraction >= 1f) { currentSize = targetSize; resizeStart = -1L }
        }
        return progress
    }

    private fun drawArtwork(canvas: Canvas, value: Drawable) {
        artworkBounds.set(value.bounds)
        try { value.bounds = imageRect; value.draw(canvas) } finally { value.bounds = artworkBounds }
    }

    private fun setColors(value: MediaViewColorConfig) {
        currentBgStartColor = value.bgStartColor
        currentBgEndColor = value.bgEndColor
    }

    override fun setAlpha(alpha: Int) { opacity = alpha.coerceIn(0, 255); invalidateSelf() }
    override fun getAlpha(): Int = opacity
    override fun setColorFilter(colorFilter: ColorFilter?) { filter = colorFilter; invalidateSelf() }
    override fun getColorFilter(): ColorFilter? = filter
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00ffffff) or (alpha shl 24)
    private fun blend(from: Int, to: Int, fraction: Float): Int {
        val alpha = blendChannel(from ushr 24, to ushr 24, fraction)
        val red = blendChannel(from ushr 16, to ushr 16, fraction)
        val green = blendChannel(from ushr 8, to ushr 8, fraction)
        val blue = blendChannel(from, to, fraction)
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun blendChannel(from: Int, to: Int, fraction: Float): Int =
        ((from and 255) + ((to and 255) - (from and 255)) * fraction)
            .toInt()
            .coerceIn(0, 255)
}
