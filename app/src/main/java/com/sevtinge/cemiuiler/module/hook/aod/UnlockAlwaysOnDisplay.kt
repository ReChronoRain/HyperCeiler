package com.sevtinge.cemiuiler.module.hook.aod

import com.sevtinge.cemiuiler.module.base.BaseHook

object UnlockAlwaysOnDisplay : BaseHook() {
    override fun init() {
        findAndHookMethod("com.miui.aod.widget.AODSettings", "onlySupportKeycodeGoto",
            object : MethodHook() {
                override fun before(param: MethodHookParam?) {
                    param?.result = false
                }
            })
    }
}
