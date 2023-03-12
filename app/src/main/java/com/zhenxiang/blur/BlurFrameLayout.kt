package com.zhenxiang.blur

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

class BlurFrameLayout constructor(context: Context) : FrameLayout(context) {
    @RequiresApi(Build.VERSION_CODES.S)
    val blurController: SystemBlurController = SystemBlurController(this)
}
