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

import android.animation.ArgbEvaluator
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.MediaViewColorConfig

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/MediaControlBgDrawable.kt
abstract class MediaControlBgDrawable(
    protected var artwork: Drawable,
    protected var colorConfig: MediaViewColorConfig,
    protected val useAnim: Boolean = true
) : Drawable() {
    protected var background: ColorDrawable = ColorDrawable()
    protected val argbEvaluator = ArgbEvaluator()
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

    abstract fun updateAlbumCover(artwork: Drawable, colorConfig: MediaViewColorConfig)

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
}
