package com.zhenxiang.blur

import android.content.Context
import android.widget.LinearLayout

class BlurLinearLayout constructor(context: Context) : LinearLayout(context) {
    val blurController: SystemBlurController = SystemBlurController(this)
}
