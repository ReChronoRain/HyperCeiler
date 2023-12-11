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
