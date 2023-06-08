package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object AlwaysShowStatusClock : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_show_status_clock")) return
        val mWorkspaceClass = loadClass("com.miui.home.launcher.Workspace")
        try {
            mWorkspaceClass.methodFinder().first {
                name == "isScreenHasClockGadget"
            }
        } catch (e: Exception) {
            mWorkspaceClass.methodFinder().first {
                name == "isScreenHasClockWidget"
            }
        } catch (e: Exception) {
            mWorkspaceClass.methodFinder().first {
                name == "isClockWidget"
            }
        }.createHook {
            before {
                it.result = false
            }
        }
    }
}
