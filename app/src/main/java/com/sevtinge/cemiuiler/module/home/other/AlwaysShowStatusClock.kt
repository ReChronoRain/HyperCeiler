package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement

object AlwaysShowStatusClock : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_show_status_clock")) return
        try {
            findMethod("com.miui.home.launcher.Workspace") {
                name == "isScreenHasClockGadget" && parameterCount == 1
            }

            /*findAndHookMethod("com.miui.home.launcher.Workspace", "isScreenHasClockGadget", 1) */
        } catch (e: Exception) {
            findMethod("com.miui.home.launcher.Workspace") {
                name == "isScreenHasClockWidget" && parameterCount == 1
            }

            /*findAndHookMethod("com.miui.home.launcher.Workspace", "isScreenHasClockWidget", 1)*/
        }.hookReturnConstant(false)
        /*object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam?): Any {
                return false
            }
        }*/
    }
}
