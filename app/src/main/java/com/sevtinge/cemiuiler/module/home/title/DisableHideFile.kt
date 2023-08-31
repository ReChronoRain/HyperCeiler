package com.sevtinge.cemiuiler.module.home.title

import android.content.ComponentName
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.IS_INTERNATIONAL_BUILD
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object DisableHideFile : BaseHook() {
    override fun init() {
        if (IS_INTERNATIONAL_BUILD) return

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
                        it.packageName == "com.google.android.documentsui"
                    }
                }
            }
        )
    }
}
