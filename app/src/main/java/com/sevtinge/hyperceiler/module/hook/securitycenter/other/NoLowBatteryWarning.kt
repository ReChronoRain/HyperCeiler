package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.provider.Settings
import com.sevtinge.hyperceiler.module.base.BaseHook

object NoLowBatteryWarning : BaseHook() {
    override fun init() {
        val settingHook: MethodHook = object : MethodHook() {
            override fun before(param: MethodHookParam) {
                val key = param.args[1] as String
                if ("low_battery_dialog_disabled" == key) param.result = 1
                else if ("low_battery_sound" == key) param.result = null
            }
        }
        hookAllMethods(Settings.System::class.java, "getInt", settingHook)
        hookAllMethods(Settings.Global::class.java, "getString", settingHook)
    }
}
