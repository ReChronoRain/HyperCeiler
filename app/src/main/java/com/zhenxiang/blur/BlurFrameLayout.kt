package com.zhenxiang.blur

import android.content.Context
import android.widget.FrameLayout

class BlurFrameLayout constructor(context: Context) : FrameLayout(context) {
    val blurController: SystemBlurController = SystemBlurController(this)
}
