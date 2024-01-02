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
