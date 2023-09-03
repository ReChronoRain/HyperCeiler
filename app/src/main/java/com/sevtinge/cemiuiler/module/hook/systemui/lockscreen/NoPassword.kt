package com.sevtinge.cemiuiler.module.hook.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object NoPassword : BaseHook() {
    override fun init() {
        loadClass("com.android.internal.widget.LockPatternUtils\$StrongAuthTracker").methodFinder().first {
            name == "isBiometricAllowedForUser"
        }.createHook {
            before {
                it.result = true
            }
        }

        loadClass("com.android.internal.widget.LockPatternUtils").methodFinder().first {
            name == "isBiometricAllowedForUser"
        }.createHook {
            before {
                it.result = true
            }
        }
    }

}
