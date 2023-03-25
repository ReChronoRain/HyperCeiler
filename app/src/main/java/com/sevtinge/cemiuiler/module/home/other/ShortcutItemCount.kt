package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod

object ShortcutItemCount : BaseHook() {
    override fun init() {

        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxCountInCurrentOrientation" }.hookAfter {
            it.result = 20
        }
        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxShortcutItemCount" }.hookAfter {
            it.result = 20
        }
        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxVisualHeight" }.hookAfter {
            it.result = it.thisObject.callMethod("getItemHeight")
        }

    }
}