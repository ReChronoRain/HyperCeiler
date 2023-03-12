package com.sevtinge.cemiuiler.module.home.recent

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object HideStatusBarWhenEnterRecent : BaseHook() {
    override fun init() {

        if (mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view")) {
            findMethod("com.miui.home.launcher.common.DeviceLevelUtils") {
                name == "isHideStatusBarWhenEnterRecents"
            }.hookReturnConstant(true)
            findMethod("com.miui.home.launcher.DeviceConfig") {
                name == "keepStatusBarShowingForBetterPerformance"
            }.hookReturnConstant(false)
        } else {
            findMethod("com.miui.home.launcher.common.DeviceLevelUtils") {
                name == "isHideStatusBarWhenEnterRecents"
            }.hookReturnConstant(false)
        }

    }
}