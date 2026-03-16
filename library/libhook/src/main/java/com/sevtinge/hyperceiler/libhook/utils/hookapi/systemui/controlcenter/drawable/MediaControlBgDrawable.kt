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

import android.animation.ArgbEvaluator
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.linearInterpolate
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/MediaControlBgDrawable.kt
abstract class MediaControlBgDrawable(
    protected var artwork: Drawable,
    protected var colorConfig: MediaViewColorConfig,
    protected val useAnim: Boolean = true
) : Drawable() {
    protected var background: ColorDrawable = ColorDrawable()
    protected var nextArtwork: Drawable? = null

    protected var albumState: AnimationState = AnimationState.DONE
    protected var albumStartTimeMillis: Long = 0
    protected val albumDuration = 333L

    protected var resizeState: AnimationState = AnimationState.DONE
    protected var resizeStartTimeMillis: Long = 0
    protected val resizeDuration = 234L
    protected var sourceSize: Int = 0
    protected var currentSize: Int = 0
    protected var targetSize: Int = 0

    protected var useResizeAnim = false

    // 复用 Rect，避免在 draw() 中每帧创建新对象
    protected val drawBoundsRect = Rect()
    protected val artworkBoundsRect = Rect()

    fun setResizeAnim(enabled: Boolean) {
        useResizeAnim = enabled
    }

    abstract fun updateAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig,
        skipAnim: Boolean = false
    )

    /**
     * 推进专辑封面过渡动画，返回当前 alpha 值 (0~255)。
     * 子类在 draw() 中调用此方法替代重复的状态机代码。
     * @param onColorProgress 在 RUNNING 阶段回调 normalized 进度，供子类插值颜色
     */
    protected fun advanceAlbumAnimation(onColorProgress: ((Float) -> Unit)? = null): Int {
        val now = SystemClock.elapsedRealtime()
        var alpha = 255
        when (albumState) {
            AnimationState.STARTING -> {
                albumStartTimeMillis = now
                albumState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (albumStartTimeMillis >= 0) {
                    val normalized = ((now - albumStartTimeMillis) / albumDuration.toFloat()).coerceIn(0.0f, 1.0f)
                    onColorProgress?.invoke(normalized)
                    alpha = linearInterpolate(0, 255, normalized)
                    if (normalized >= 1.0f) {
                        albumState = AnimationState.DONE
                        artwork = nextArtwork ?: artwork
                        nextArtwork = null
                        alpha = 255
                    }
                }
            }
            else -> {}
        }
        return alpha
    }

    /**
     * 推进 resize 动画，自动更新 currentSize。
     */
    protected fun advanceResizeAnimation() {
        val now = SystemClock.elapsedRealtime()
        when (resizeState) {
            AnimationState.STARTING -> {
                resizeStartTimeMillis = now
                resizeState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (resizeStartTimeMillis >= 0) {
                    val normalized = ((now - resizeStartTimeMillis) / resizeDuration.toFloat()).coerceIn(0.0f, 1.0f)
                    currentSize = linearInterpolate(sourceSize, targetSize, normalized)
                    if (normalized >= 1.0f) {
                        resizeState = AnimationState.DONE
                        currentSize = targetSize
                    }
                }
            }
            else -> {}
        }
    }

    /**
     * 如果动画仍在进行中，调用 invalidateSelf() 触发下一帧。
     */
    protected fun scheduleNextFrameIfAnimating() {
        if (albumState != AnimationState.DONE || resizeState != AnimationState.DONE) {
            invalidateSelf()
        }
    }

    override fun setAlpha(p0: Int) {
        artwork.alpha = p0
        background.alpha = p0
        invalidateSelf()
    }

    override fun setColorFilter(p0: ColorFilter?) {
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return background.opacity
    }

    companion object {
        /** 所有子类共享的 ArgbEvaluator 实例 */
        @JvmStatic
        protected val argbEvaluator = ArgbEvaluator()
    }
}
