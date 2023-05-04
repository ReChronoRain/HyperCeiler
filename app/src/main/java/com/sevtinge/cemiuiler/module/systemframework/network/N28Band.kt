package com.sevtinge.cemiuiler.module.systemframework.network

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object N28Band : BaseHook() {
    override fun init() {
        try {
            findMethod("miui.telephony.TelephonyManagerEx") {
                name == "isN28Supported"
            }.hookReturnConstant(true)
        } catch (_: Throwable) {
        }
    }
}
