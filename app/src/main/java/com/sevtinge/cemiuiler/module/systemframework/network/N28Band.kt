package com.sevtinge.cemiuiler.module.systemframework.network

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object N28Band : BaseHook() {
    override fun init() {
        try {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isN28Supported"
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
    }
}
