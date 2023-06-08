package com.sevtinge.cemiuiler.module.systemframework.network

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object N5N8Band : BaseHook() {
    override fun init() {
        try {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isN5Supported"
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }

        try {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isN8Supported"
            }.createHook {
                returnConstant(true)
            }
        } catch (_: Throwable) {
        }
    }
}