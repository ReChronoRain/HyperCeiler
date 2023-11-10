package com.sevtinge.hyperceiler.module.hook.systemsettings

import com.sevtinge.hyperceiler.module.base.BaseHook

object UnLockAreaScreenshot : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.android.settings.MiuiShortcut\$System", "supportPartialScreenShot",
            object : MethodHook() {
                override fun before(param: MethodHookParam?) {
                    param?.result = true
                }
            })
    }
}
