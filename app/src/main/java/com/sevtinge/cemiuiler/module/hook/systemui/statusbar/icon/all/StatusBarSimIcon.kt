package com.sevtinge.cemiuiler.module.hook.systemui.statusbar.icon.all

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object StatusBarSimIcon : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.phone.StatusBarSignalPolicy").methodFinder().first {
            name == "hasCorrectSubs" && parameterTypes[0] == MutableList::class.java
        }.createHook {
            before {
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

}
