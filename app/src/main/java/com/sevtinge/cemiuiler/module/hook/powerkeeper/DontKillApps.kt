package com.sevtinge.cemiuiler.module.hook.powerkeeper

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DontKillApps : BaseHook() {
    override fun init() {
        loadClass("miui.process.ProcessManager").methodFinder().first {
            name == "kill"
        }.createHook {
            before {
                it.result = false
            }
        }
    }
}
