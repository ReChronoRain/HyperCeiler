package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook

object MobileTypeTextCustom : BaseHook() {
    override fun init() {
        findMethod("com.android.systemui.statusbar.connectivity.MobileSignalController") {
            name == "getMobileTypeName" && parameterTypes[0] == Int::class.java
        }.hookAfter {
            it.result = mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "5G")
        }
    }
}