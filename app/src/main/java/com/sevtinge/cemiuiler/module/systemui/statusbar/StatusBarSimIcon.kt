package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object StatusBarSimIcon : BaseHook() {
    override fun init() {
        findMethod("com.android.systemui.statusbar.phone.StatusBarSignalPolicy") {
            name == "hasCorrectSubs" && parameterTypes[0] == MutableList::class.java
        }.hookBefore {
            val list = it.args[0] as MutableList<*>
           /* val size = list.size*/
            if (mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_card_2", 0) == 2) {
                list.removeAt(1)
            }
            if (mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_card_1", 0) == 2) {
                list.removeAt(0)
            }
        }
    }

}