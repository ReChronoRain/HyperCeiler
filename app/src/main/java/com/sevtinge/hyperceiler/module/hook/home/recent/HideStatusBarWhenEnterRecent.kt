package com.sevtinge.hyperceiler.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object HideStatusBarWhenEnterRecent : BaseHook() {
    override fun init() {
        val mDeviceLevelClass = loadClass("com.miui.home.launcher.common.DeviceLevelUtils")

        // 不应该在默认情况下强制显示
        // if (mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view")) {
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
        // } else {
        //     mDeviceLevelClass.methodFinder().first {
        //         name == "isHideStatusBarWhenEnterRecents"
        //     }.createHook {
        //         returnConstant(false)
        //     }
        // }
    }
}
