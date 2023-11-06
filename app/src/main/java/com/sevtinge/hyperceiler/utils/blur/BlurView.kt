package com.yuk.hyperOS_XXL.blur

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.sevtinge.hyperceiler.utils.api.dp2px
import com.yuk.hyperOS_XXL.blur.MiBlurUtilities.mSupportedMiBlur

@SuppressLint("ViewConstructor")
class BlurView(context: Context, private val radius: Int) : View(context) {

    private fun setBlur() {
        //MiBlurUtilities.resetBlurColor(this.parent as View)
        MiBlurUtilities.setPassWindowBlurEnable(this.parent as View, true)
        MiBlurUtilities.setViewBlur(this.parent as View, 1)
        MiBlurUtilities.setBlurRoundRect(this.parent as View, dp2px(context, radius.toFloat()))
        //MiBlurUtilities.setBlurColor(this.parent as View, if (isDarkMode(this.context)) 0x14ffffff else 0x14000000, 3)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mSupportedMiBlur) {
            setBlur()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        MiBlurUtilities.clearAllBlur(this.parent as View)
    }
}
