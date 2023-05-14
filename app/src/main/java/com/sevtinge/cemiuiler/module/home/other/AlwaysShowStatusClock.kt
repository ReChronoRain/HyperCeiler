package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object AlwaysShowStatusClock : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_show_status_clock")) return
        try {
            loadClass("com.miui.home.launcher.Workspace").methodFinder().first {
                name == "isScreenHasClockGadget" && parameterCount == 1
            }
        } catch (e: Exception) {
            loadClass("com.miui.home.launcher.Workspace").methodFinder().first {
                name == "isScreenHasClockWidget" && parameterCount == 1
            }
        }.createHook {
            before { param ->
                param.result = false
            }
        }

    }
}
