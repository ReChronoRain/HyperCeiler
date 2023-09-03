package com.sevtinge.cemiuiler.module.hook.systemui.statusbar

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object MobileTypeTextCustom : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController").methodFinder().first {
            name == "getMobileTypeName" && parameterTypes[0] == Int::class.java
        }.createHook {
            after {
                it.result = mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "ERR")
            }
        }
    }
}
