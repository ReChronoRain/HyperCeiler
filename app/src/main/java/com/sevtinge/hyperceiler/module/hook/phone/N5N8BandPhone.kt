package com.sevtinge.hyperceiler.module.hook.phone

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object N5N8BandPhone : BaseHook() {
    override fun init() {
        runCatching {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isN5Supported"
            }.createHook {
                returnConstant(true)
            }
        }

        runCatching {
            loadClass("miui.telephony.TelephonyManagerEx").methodFinder().first {
                name == "isN8Supported"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
