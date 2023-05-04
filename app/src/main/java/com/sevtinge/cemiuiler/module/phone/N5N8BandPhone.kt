package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object N5N8BandPhone : BaseHook() {
    override fun init() {
        try {
            findMethod("miui.telephony.TelephonyManagerEx") {
                name == "isN5Supported"
            }.hookReturnConstant(true)
        } catch (_: Throwable) {
        }

        try {
            findMethod("miui.telephony.TelephonyManagerEx") {
                name == "isN8Supported"
            }.hookReturnConstant(true)
        } catch (_: Throwable) {
        }
    }
}