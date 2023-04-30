package com.sevtinge.cemiuiler.module.powerkeeper

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object DontKillApps : BaseHook() {
    override fun init() {
        findMethod("miui.process.ProcessManager") {
            name == "kill"
        }.hookBefore {
            it.result = false
        }
    }
}