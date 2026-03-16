/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import kotlin.math.min

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/LinearGradientDrawable.kt
class LinearGradientDrawable(
    artwork: Drawable,
    colorConfig: MediaViewColorConfig,
    useAnim: Boolean = true
) : MediaControlBgDrawable(artwork, colorConfig, useAnim) {
    private var gradient: GradientDrawable = GradientDrawable()

    private var sourceColor: Int = colorConfig.bgStartColor
    private var currentColor: Int = colorConfig.bgStartColor
    private var targetColor: Int = colorConfig.bgStartColor

    init {
        gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradient.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        gradient.shape = GradientDrawable.RECTANGLE
        background.color = currentColor
        gradient.colors = intArrayOf(
            currentColor and 0x00ffffff or (51 shl 24),
            currentColor and 0x00ffffff or (51 shl 24)
        )
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val newSize = min(bounds.width(), bounds.height())
        if (currentSize == 0) {
            currentSize = newSize
        }
        if (useAnim && useResizeAnim) {
            sourceSize = currentSize
            targetSize = newSize
            resizeState = AnimationState.STARTING
        } else {
            sourceSize = newSize
            currentSize = newSize
            resizeState = AnimationState.DONE
        }
        invalidateSelf()
    }

    override fun draw(p0: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        val alpha = advanceAlbumAnimation { normalized ->
            currentColor = argbEvaluator.evaluate(normalized, sourceColor, targetColor) as Int
            if (normalized >= 1.0f) currentColor = targetColor
        }
        advanceResizeAnimation()

        background.color = currentColor
        background.setBounds(0, 0, bounds.width(), bounds.height())
        background.draw(p0)

        drawBoundsRect.set(bounds.width() - currentSize, 0, bounds.width(), currentSize)
        // 内缩左边缘 1px（与纯色背景的接壤处），渐变的不透明左端覆盖此间隙
        artworkBoundsRect.set(
            drawBoundsRect.left + 1, drawBoundsRect.top,
            drawBoundsRect.right, drawBoundsRect.bottom
        )

        if (alpha == 0 || alpha == 255) {
            artwork.bounds = artworkBoundsRect
            artwork.draw(p0)
        } else {
            artwork.bounds = artworkBoundsRect
            artwork.alpha = 255 - alpha
            artwork.draw(p0)
            artwork.alpha = 255
            nextArtwork?.let {
                it.bounds = artworkBoundsRect
                it.alpha = alpha
                it.draw(p0)
                it.alpha = 255
            }
        }
        gradient.colors = intArrayOf(
            currentColor,
            currentColor and 0x00ffffff or (51 shl 24),
        )
        gradient.setBounds(bounds.width() - currentSize, 0, bounds.width(), bounds.height())
        gradient.draw(p0)
        scheduleNextFrameIfAnimating()
    }

    override fun updateAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig,
        skipAnim: Boolean
    ) {
        val hostView = callback as? View
        val shouldSnap = !useAnim || skipAnim || hostView == null || !hostView.isShown || !hostView.isAttachedToWindow

        if (shouldSnap) {
            nextArtwork = null
            this.artwork = artwork
            sourceColor = colorConfig.bgStartColor
            currentColor = colorConfig.bgStartColor
            albumState = AnimationState.DONE
            this.colorConfig = colorConfig
        } else {
            nextArtwork = artwork
            sourceColor = currentColor
            targetColor = colorConfig.bgStartColor
            albumState = AnimationState.STARTING
            this.colorConfig = colorConfig
        }
        invalidateSelf()
    }
}
