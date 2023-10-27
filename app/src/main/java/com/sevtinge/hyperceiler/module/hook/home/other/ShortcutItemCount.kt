package com.sevtinge.hyperceiler.module.hook.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod

object ShortcutItemCount : BaseHook() {
    override fun init() {
        val mAppShortcutMenuClass = loadClass("com.miui.home.launcher.shortcuts.AppShortcutMenu")

        mAppShortcutMenuClass.methodFinder().first {
            name == "getMaxCountInCurrentOrientation"
        }.createHook {
            after {
                it.result = 20
            }
        }

        mAppShortcutMenuClass.methodFinder().first {
            name == "getMaxShortcutItemCount"
        }.createHook {
            after {
                it.result = 20
            }
        }

        mAppShortcutMenuClass.methodFinder().first {
            name == "getMaxVisualHeight"
        }.createHook {
            after {
                it.result = it.thisObject.callMethod("getItemHeight")
            }
        }

    }
}
