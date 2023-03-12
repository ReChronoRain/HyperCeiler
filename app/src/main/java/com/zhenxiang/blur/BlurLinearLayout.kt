package com.zhenxiang.blur

import android.content.Context
import android.os.Build
import android.widget.LinearLayout
import androidx.annotation.RequiresApi

class BlurLinearLayout constructor(context: Context) : LinearLayout(context) {
    @RequiresApi(Build.VERSION_CODES.S)
    val blurController: SystemBlurController = SystemBlurController(this)
}