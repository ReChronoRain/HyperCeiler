package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object NoPasswordHook : BaseHook() {
    override fun init() {
        findMethod("com.android.internal.widget.LockPatternUtils\$StrongAuthTracker") {
            name == "isBiometricAllowedForUser"
        }.hookBefore {
            it.result = true
        }

        findMethod("com.android.internal.widget.LockPatternUtils") {
            name == "isBiometricAllowedForUser"
        }.hookBefore {
            it.result = true
        }
    }

}