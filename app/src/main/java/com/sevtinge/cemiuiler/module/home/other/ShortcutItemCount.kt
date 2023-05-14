package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod

object ShortcutItemCount : BaseHook() {
    override fun init() {

        loadClass("com.miui.home.launcher.shortcuts.AppShortcutMenu").methodFinder().first {
            name == "getMaxCountInCurrentOrientation"
        }.createHook {
            after {
                it.result = 20
            }
        }

        loadClass("com.miui.home.launcher.shortcuts.AppShortcutMenu").methodFinder().first {
            name == "getMaxShortcutItemCount"
        }.createHook {
            after {
                it.result = 20
            }
        }

        loadClass("com.miui.home.launcher.shortcuts.AppShortcutMenu").methodFinder().first {
            name == "getMaxVisualHeight"
        }.createHook {
            after {
                it.result = it.thisObject.callMethod("getItemHeight")
            }
        }


    }
}