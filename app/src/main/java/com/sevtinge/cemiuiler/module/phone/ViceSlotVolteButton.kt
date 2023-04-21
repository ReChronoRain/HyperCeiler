package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.exec
import de.robv.android.xposed.XposedBridge

object ViceSlotVolteButton : BaseHook() {
    override fun init() {
        try {
            exec("settings put global vice_slot_volte_data_enabled 1")
            findMethod("com.android.phone.MiuiPhoneUtils") {
                name == "shouldHideViceSlotVolteDataButton"
            }.hookReturnConstant(false)
        } catch (_: Throwable) {
        }
        try {
            findMethod("com.android.phone.MiuiPhoneUtils") {
                name == "shouldHideSmartDualSimButton"
            }.hookReturnConstant(false)
        } catch (_: Throwable) {
        }
    }
}