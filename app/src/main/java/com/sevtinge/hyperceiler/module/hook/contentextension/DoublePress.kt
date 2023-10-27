package com.sevtinge.hyperceiler.module.hook.contentextension

import android.content.Context
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

class DoublePress : BaseHook() {
    override fun init() {
        loadClass("com.miui.contentextension.utils.ContentCatcherUtil").methodFinder().first {
            name == "isCatcherSupportDoublePress" &&
                parameterTypes[0] == Context::class.java
        }.createHook {
            returnConstant(true)
        }
    }
}
