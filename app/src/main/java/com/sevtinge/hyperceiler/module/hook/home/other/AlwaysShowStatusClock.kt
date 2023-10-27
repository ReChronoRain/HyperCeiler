package com.sevtinge.hyperceiler.module.hook.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object AlwaysShowStatusClock : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_show_status_clock")) return
        val mWorkspaceClass = loadClass("com.miui.home.launcher.Workspace")
        val methodNames =
            listOf("isScreenHasClockGadget", "isScreenHasClockWidget", "isClockWidget")

        methodNames.forEach { methodName ->
            val result = runCatching {
                mWorkspaceClass.methodFinder().first {
                    name == methodName
                }.createHook {
                    before { it.result = false }
                }
            }
            if (result.isSuccess) return@forEach
        }
    }
}
