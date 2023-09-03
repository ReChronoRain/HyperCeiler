package com.sevtinge.cemiuiler.module.hook.powerkeeper

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object LockMaxFps : BaseHook() {
    override fun init() {
        loadClass("com.miui.powerkeeper.statemachine.DisplayFrameSetting").methodFinder().first {
            name == "setScreenEffect" && parameterCount == 3
        }.createHook {
            before {
                it.result = null
            }
        }
    }
}
