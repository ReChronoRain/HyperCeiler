package com.sevtinge.cemiuiler.module.contentextension

import android.graphics.Bitmap
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

class SuperImage : BaseHook() {
    override fun init() {
        findMethod("com.miui.contentextension.utils.SuperImageUtils") {
            name == "isSupportSuperImage"
        }.hookReturnConstant(true)
        findMethod("com.miui.contentextension.utils.SuperImageUtils") {
            name == "isBitmapSupportSuperImage" &&
                    parameterTypes[0] == Bitmap::class.java
        }.hookReturnConstant(true)
    }
}