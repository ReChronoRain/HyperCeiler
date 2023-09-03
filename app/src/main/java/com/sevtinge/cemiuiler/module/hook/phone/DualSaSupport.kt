package com.sevtinge.cemiuiler.module.hook.phone

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DualSaSupport : BaseHook() {
    override fun init() {
        loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
            name == "isDualSaSupported"
        }.createHook {
            returnConstant(true)
        }
    }
}
