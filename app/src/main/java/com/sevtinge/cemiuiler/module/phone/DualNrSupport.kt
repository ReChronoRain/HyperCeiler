package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object DualNrSupport : BaseHook() {
    override fun init() {
        try {
            findMethod("miui.telephony.TelephonyManagerEx") {
                name == "isDualNrSupported"
            }.hookReturnConstant(true)
        } catch (_: Throwable) {
        }
    }
}