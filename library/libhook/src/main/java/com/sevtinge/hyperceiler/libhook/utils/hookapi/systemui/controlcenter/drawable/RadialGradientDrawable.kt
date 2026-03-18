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
import androidx.core.graphics.withClip
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import kotlin.math.abs
import kotlin.math.max

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/RadialGradientDrawable.kt
class RadialGradientDrawable(
    artwork: Drawable,
    colorConfig: MediaViewColorConfig,
    useAnim: Boolean = true
) : MediaControlBgDrawable(artwork, colorConfig, useAnim) {
    private var gradient: GradientDrawable = GradientDrawable()

    private var sourceStartColor: Int = colorConfig.bgStartColor
    private var currentStartColor: Int = colorConfig.bgStartColor
    private var targetStartColor: Int = colorConfig.bgStartColor
    private var sourceEndColor: Int = colorConfig.bgEndColor
    private var currentEndColor: Int = colorConfig.bgEndColor
    private var targetEndColor: Int = colorConfig.bgEndColor

    init {
        gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
        gradient.shape = GradientDrawable.RECTANGLE
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        gradient.gradientRadius = max(bounds.width(), bounds.height()).toFloat()
        val newSize = abs(bounds.width() - bounds.height()) / 2
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

        val w = bounds.width()
        val h = bounds.height()
        val squareSize = max(w, h)

        val alpha = advanceAlbumAnimation { normalized ->
            currentStartColor = argbEvaluator.evaluate(normalized, sourceStartColor, targetStartColor) as Int
            currentEndColor = argbEvaluator.evaluate(normalized, sourceEndColor, targetEndColor) as Int
            if (normalized >= 1.0f) {
                currentStartColor = targetStartColor
                currentEndColor = targetEndColor
            }
        }
        advanceResizeAnimation()

        // 裁剪到 View 实际边界，保证 outline 圆角生效
        p0.withClip(bounds) {
            background.color = currentStartColor
            background.setBounds(0, 0, squareSize, squareSize)
            background.draw(this)

            drawBoundsRect.set(0, -currentSize, squareSize, squareSize - currentSize)
            artworkBoundsRect.set(
                drawBoundsRect.left + 1, drawBoundsRect.top + 1,
                drawBoundsRect.right - 1, drawBoundsRect.bottom - 1
            )

            if (alpha == 0 || alpha == 255) {
                artwork.bounds = artworkBoundsRect
                artwork.draw(this)
            } else {
                artwork.bounds = artworkBoundsRect
                artwork.alpha = 255 - alpha
                artwork.draw(this)
                artwork.alpha = 255
                nextArtwork?.let {
                    it.bounds = artworkBoundsRect
                    it.alpha = alpha
                    it.draw(this)
                    it.alpha = 255
                }
            }
            gradient.colors = intArrayOf(
                currentStartColor and 0x00ffffff or (64 shl 24),
                currentEndColor and 0x00ffffff or (255 shl 24)
            )
            gradient.bounds = drawBoundsRect
            gradient.draw(this)

        }
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
            sourceStartColor = colorConfig.bgStartColor
            currentStartColor = colorConfig.bgStartColor
            sourceEndColor = colorConfig.bgEndColor
            currentEndColor = colorConfig.bgEndColor
            albumState = AnimationState.DONE
            this.colorConfig = colorConfig
        } else {
            nextArtwork = artwork
            sourceStartColor = currentStartColor
            targetStartColor = colorConfig.bgStartColor
            sourceEndColor = currentEndColor
            targetEndColor = colorConfig.bgEndColor
            albumState = AnimationState.STARTING
            this.colorConfig = colorConfig
        }
        invalidateSelf()
    }
}
