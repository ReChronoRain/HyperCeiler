package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

// by ljlvink
object DisableMiuiMultiWinSwitch : BaseHook() {
    override fun init() {
        loadClass("com.android.wm.shell.miuimultiwinswitch.miuiwindowdecor.MiuiDotView", lpparam.classLoader).methodFinder().first {
            name == "onDraw"
        }.createHook {
            before {
                it.result = null
            }
        }
    }
}
