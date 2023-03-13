package com.sevtinge.cemiuiler.module.contentextension

import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

class DoublePress : BaseHook() {
    override fun init() {
        findMethod("com.miui.contentextension.utils.ContentCatcherUtil") {
            name == "isCatcherSupportDoublePress" &&
                    parameterTypes[0] == Context::class.java
        }.hookReturnConstant(true)
    }
}