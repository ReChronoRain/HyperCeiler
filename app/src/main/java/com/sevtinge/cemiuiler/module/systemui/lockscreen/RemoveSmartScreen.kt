package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hasEnable

object RemoveSmartScreen : BaseHook() {
    override fun init() {
        findMethod("com.android.keyguard.negative.MiuiKeyguardMoveLeftViewContainer") {
            name == "inflateLeftView"
        }.hookBefore {
            it.result = null
        }
    }

}