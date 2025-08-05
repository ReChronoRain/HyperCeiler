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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.MediaViewColorConfig
import com.sevtinge.hyperceiler.hook.utils.api.HyperHelperApis.linearInterpolate
import kotlin.math.abs

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/TransitionDrawable.kt
class TransitionDrawable(
    artwork: Drawable,
    colorConfig: MediaViewColorConfig,
    useAnim: Boolean = true
) : MediaControlBgDrawable(artwork, colorConfig, useAnim) {
    private var sourceColor: Int = colorConfig.bgStartColor
    private var currentColor: Int = colorConfig.bgStartColor
    private var targetColor: Int = colorConfig.bgStartColor

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val newSize = abs(bounds.width() - bounds.height()) / 2
        if (currentSize == 0) {
            currentSize = newSize
        }
        sourceSize = currentSize
        targetSize = newSize
        resizeState = AnimationState.STARTING
        invalidateSelf()
    }

    override fun draw(p0: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        var alpha = 255
        when (albumState) {
            AnimationState.STARTING -> {
                albumStartTimeMillis = System.currentTimeMillis()
                albumState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (albumStartTimeMillis >= 0) {
                    val normalized: Float = ((System.currentTimeMillis() - albumStartTimeMillis) / albumDuration.toFloat()).coerceIn(0.0f, 1.0f)
                    currentColor = argbEvaluator.evaluate(normalized, sourceColor, targetColor) as Int
                    alpha = linearInterpolate(0, 255, normalized)
                    if (normalized >= 1.0f || !useAnim) {
                        albumState = AnimationState.DONE
                        currentColor = targetColor
                        artwork = nextArtwork ?: artwork
                        alpha = 255
                    }
                }
            }
            else -> {}
        }
        when (resizeState) {
            AnimationState.STARTING -> {
                resizeStartTimeMillis = System.currentTimeMillis()
                resizeState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (resizeStartTimeMillis >= 0) {
                    val normalized: Float = ((System.currentTimeMillis() - resizeStartTimeMillis) / resizeDuration.toFloat()).coerceIn(0.0f, 1.0f)
                    currentSize = linearInterpolate(sourceSize, targetSize, normalized)
                    if (normalized >= 1.0f || !useAnim) {
                        resizeState = AnimationState.DONE
                        currentSize = targetSize
                    }
                }
            }
            else -> {}
        }
        background.color = currentColor
        background.setBounds(0, 0, bounds.width(), bounds.width())
        background.draw(p0)
        if (alpha == 0 || alpha == 255) {
            artwork.setBounds(0, -currentSize, bounds.width(), bounds.width() - currentSize)
            artwork.draw(p0)
        } else {
            artwork.setBounds(0, -currentSize, bounds.width(), bounds.width() - currentSize)
            artwork.alpha = 255 - alpha
            artwork.draw(p0)
            artwork.alpha = 255
            nextArtwork?.let {
                it.setBounds(0, -currentSize, bounds.width(), bounds.width() - currentSize)
                it.alpha = alpha
                it.draw(p0)
                it.alpha = 255
            }
        }
        if (albumState != AnimationState.DONE || resizeState != AnimationState.DONE) {
            invalidateSelf()
        }
    }

    override fun updateAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig
    ) {
        nextArtwork = artwork
        sourceColor = currentColor
        targetColor = colorConfig.bgStartColor
        albumState = AnimationState.STARTING
        this.colorConfig = colorConfig
        invalidateSelf()
    }
}
