package com.sevtinge.cemiuiler.module.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object HideStatusBarWhenEnterRecent : BaseHook() {
    override fun init() {
        val mDeviceLevelClass = loadClass("com.miui.home.launcher.common.DeviceLevelUtils")

        if (mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view")) {
            mDeviceLevelClass.methodFinder().first {
                name == "isHideStatusBarWhenEnterRecents"
            }.createHook {
                returnConstant(true)
            }

            loadClass("com.miui.home.launcher.DeviceConfig").methodFinder().first {
                name == "keepStatusBarShowingForBetterPerformance"
            }.createHook {
                returnConstant(false)
            }
        } else {
            mDeviceLevelClass.methodFinder().first {
                name == "isHideStatusBarWhenEnterRecents"
            }.createHook {
                returnConstant(false)
            }
        }
    }
}