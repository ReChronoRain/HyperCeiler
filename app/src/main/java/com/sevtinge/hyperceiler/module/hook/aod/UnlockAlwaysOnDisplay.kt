package com.sevtinge.hyperceiler.module.hook.aod

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object UnlockAlwaysOnDisplay : BaseHook() {
    override fun init() {
        loadClass("com.miui.aod.widget.AODSettings").methodFinder().first {
            name == "onlySupportKeycodeGoto"
        }.createHook {
            returnConstant(false)
        }
    }
}
