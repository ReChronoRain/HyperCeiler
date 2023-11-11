package com.sevtinge.hyperceiler.module.hook.home.other

import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object FixAndroidRS : BaseHook() {
    override fun init() {
        val globalSearchUtilClass = "com.miui.home.launcher.GlobalSearchUtil"
        XposedHelpers.findAndHookMethod(globalSearchUtilClass, lpparam.classLoader,
            "isSupportPullDownSearch", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any {
                    return false
                }
            })

    }
}
