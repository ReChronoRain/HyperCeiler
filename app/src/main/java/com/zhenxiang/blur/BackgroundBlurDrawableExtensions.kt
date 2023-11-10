package com.zhenxiang.blur

import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun BackgroundBlurDrawable.setColor(color: Int) {
    HiddenApiBypass.invoke(BackgroundBlurDrawable::class.java, this, "setColor", color)
}

fun BackgroundBlurDrawable.setBlurRadius(blurRadius: Int) {
    HiddenApiBypass.invoke(BackgroundBlurDrawable::class.java, this, "setBlurRadius", blurRadius)
}

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
