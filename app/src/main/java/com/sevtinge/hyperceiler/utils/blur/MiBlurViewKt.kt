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
package com.sevtinge.hyperceiler.utils.blur

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.sevtinge.hyperceiler.utils.api.dp2px
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.mSupportedMiBlur

@SuppressLint("ViewConstructor")
class MiBlurViewKt(context: Context, private val radius: Int) : View(context) {

    private fun setBlur() {
        //MiBlurUtilsKt.resetBlurColor(this.parent as View)
        MiBlurUtilsKt.setPassWindowBlurEnable(this.parent as View, true)
        MiBlurUtilsKt.setViewBlur(this.parent as View, 1)
        MiBlurUtilsKt.setBlurRoundRect(this.parent as View, dp2px(context, radius.toFloat()))
        //MiBlurUtilsKt.setBlurColor(this.parent as View, if (isDarkMode(this.context)) 0x14ffffff else 0x14000000, 3)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mSupportedMiBlur) {
            setBlur()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        MiBlurUtilsKt.clearAllBlur(this.parent as View)
    }
}
