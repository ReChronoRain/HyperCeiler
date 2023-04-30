package com.sevtinge.cemiuiler.module.powerkeeper

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object LockMaxFps : BaseHook() {
    override fun init() {
        findMethod("com.miui.powerkeeper.statemachine.DisplayFrameSetting") {
            name == "setScreenEffect" && parameterCount == 3
        }.hookBefore {
            it.result = null
        }
    }
}