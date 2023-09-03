package com.sevtinge.cemiuiler.module.hook.systemframework.network

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DualNRSupport : BaseHook() {
    override fun init() {
        runCatching {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isDualNrSupported"
            }.createHook {
                before {
                    it.result = true
                }
            }
        }
    }
}
