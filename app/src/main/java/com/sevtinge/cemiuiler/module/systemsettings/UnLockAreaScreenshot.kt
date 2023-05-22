package com.sevtinge.cemiuiler.module.systemsettings

import com.sevtinge.cemiuiler.module.base.BaseHook

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