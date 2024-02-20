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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.blur.zhenxiang

import android.os.Build
import androidx.annotation.RequiresApi
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import org.lsposed.hiddenapibypass.HiddenApiBypass

@RequiresApi(Build.VERSION_CODES.S)
fun BackgroundBlurDrawable.setColor(color: Int) {
    HiddenApiBypass.invoke(BackgroundBlurDrawable::class.java, this, "setColor", color)
}

@RequiresApi(Build.VERSION_CODES.S)
fun BackgroundBlurDrawable.setBlurRadius(blurRadius: Int) {
    HiddenApiBypass.invoke(BackgroundBlurDrawable::class.java, this, "setBlurRadius", blurRadius)
}

@RequiresApi(Build.VERSION_CODES.S)
fun BackgroundBlurDrawable.setCornerRadius(
    cornerRadiusTL: Float,
    cornerRadiusTR: Float,
    cornerRadiusBL: Float,
    cornerRadiusBR: Float
) {
    HiddenApiBypass.invoke(
        BackgroundBlurDrawable::class.java,
        this,
        "setCornerRadius",
        cornerRadiusTL,
        cornerRadiusTR,
        cornerRadiusBL,
        cornerRadiusBR
    )
}
