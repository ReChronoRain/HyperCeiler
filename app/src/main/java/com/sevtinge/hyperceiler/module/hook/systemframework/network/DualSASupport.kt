package com.sevtinge.hyperceiler.module.hook.systemframework.network

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object DualSASupport : BaseHook() {
    override fun init() {
        runCatching {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isDualSaSupported"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
