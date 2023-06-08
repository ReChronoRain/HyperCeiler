package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object RemoveSmartScreen : BaseHook() {
    override fun init() {
        loadClass("com.android.keyguard.negative.MiuiKeyguardMoveLeftViewContainer").methodFinder().first {
            name == "inflateLeftView"
        }.createHook {
            before {
                it.result = null
            }
        }
    }

}