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
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import kotlin.math.max

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/RadialMaskedDrawable.kt
class RadialMaskedDrawable(
    private var artwork: Drawable,
    private var startColor: Int = Color.BLACK,
    private var endColor: Int = Color.BLACK
) : Drawable() {
    private var gradient: GradientDrawable = GradientDrawable()
    private var background: ColorDrawable = ColorDrawable()

    init {
        gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
        gradient.shape = GradientDrawable.RECTANGLE
    }

    override fun getIntrinsicWidth(): Int {
        return artwork.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return artwork.intrinsicHeight
    }

    override fun draw(p0: Canvas) {
        background.color = startColor
        background.setBounds(0, 0, bounds.width(), bounds.height())
        background.draw(p0)
        artwork.setBounds(0, 0, bounds.width(), bounds.height())
        artwork.draw(p0)
        gradient.colors = intArrayOf(
            startColor and 0x00ffffff or (64 shl 24),
            endColor and 0x00ffffff or (255 shl 24)
        )
        gradient.setBounds(0, 0, bounds.width(), bounds.height())
        gradient.gradientRadius = max(bounds.width(), bounds.height()).toFloat()
        gradient.draw(p0)
    }

    override fun setAlpha(p0: Int) {
        artwork.alpha = p0
        background.alpha = p0
        gradient.alpha = p0
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
