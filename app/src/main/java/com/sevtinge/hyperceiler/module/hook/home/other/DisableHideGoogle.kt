package com.sevtinge.hyperceiler.module.hook.home.other

import android.content.ComponentName
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import miui.os.Build

@Suppress("UNCHECKED_CAST")
object DisableHideGoogle : BaseHook() {
    override fun init() {
        if (Build.IS_INTERNATIONAL_BUILD)
            return

        XposedHelpers.findAndHookConstructor(
            "com.miui.home.launcher.AppFilter",
            lpparam.classLoader,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val skippedItem = XposedHelpers.getObjectField(
                        param.thisObject,
                        "mSkippedItems"
                    ) as HashSet<ComponentName>

                    skippedItem.removeIf {
                        it.packageName == "com.google.android.googlequicksearchbox"
                            || it.packageName == "com.google.android.gms"
                    }
                }
            }
        )
    }

}
