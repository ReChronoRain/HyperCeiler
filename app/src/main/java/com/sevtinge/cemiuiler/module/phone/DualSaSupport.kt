package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object DualSaSupport : BaseHook() {
    override fun init() {
        try {
            findMethod("miui.telephony.TelephonyManagerEx") {
                name == "isDualSaSupported"
            }.hookReturnConstant(true)
        } catch (_: Throwable) {
        }
    }
}