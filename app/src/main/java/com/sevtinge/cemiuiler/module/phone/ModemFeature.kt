package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook

object ModemFeature : BaseHook() {
    override fun init() {
        try {
            findMethod("com.android.phone.FiveGManagerBase") {
                name == "getModemFeatureMode"
            }.hookAfter {
                it.args[0] = -1
                it.result = true
            }
        } catch (_: Throwable) {
        }

        try {
            findMethod("com.android.phone.MiuiPhoneUtils") {
                name == "isModemFeatureSupported"
            }.hookAfter {
                it.args[0] = -1
            }
        } catch (_: Throwable) {
        }

        try {
            findMethod("com.android.phone.MiuiPhoneUtils") {
                name == "getModemFeatureFromDb"
            }.hookAfter {
                it.args[0] = -1
            }
        } catch (_: Throwable) {
        }
    }
}