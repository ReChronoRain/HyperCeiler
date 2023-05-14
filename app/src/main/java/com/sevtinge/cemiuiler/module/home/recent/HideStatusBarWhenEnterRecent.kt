package com.sevtinge.cemiuiler.module.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object HideStatusBarWhenEnterRecent : BaseHook() {
    override fun init() {

        if (mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view")) {
            loadClass("com.miui.home.launcher.common.DeviceLevelUtils").methodFinder().first {
                name == "isHideStatusBarWhenEnterRecents"
            }.createHook {
                before {
                    it.result = true
                }
            }
            loadClass("com.miui.home.launcher.DeviceConfig").methodFinder().first {
                name == "keepStatusBarShowingForBetterPerformance"
            }.createHook {
                before {
                    it.result = false
                }
            }
        } else {
            loadClass("com.miui.home.launcher.common.DeviceLevelUtils").methodFinder().first {
                name == "isHideStatusBarWhenEnterRecents"
            }.createHook {
                before {
                    it.result = false
                }
            }
        }

    }
}