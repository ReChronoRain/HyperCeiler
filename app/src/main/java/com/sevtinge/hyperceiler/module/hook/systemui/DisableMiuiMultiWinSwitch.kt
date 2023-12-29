package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object DisableMiuiMultiWinSwitch : BaseHook() {
    override fun init() {
        loadClass("com.android.wm.shell.miuimultiwinswitch.MiuiMultiWinSwitch", lpparam.classLoader).methodFinder().first {
            name == "isSupported"
        }.createHook {
            returnConstant(false)
        }
    }
}
