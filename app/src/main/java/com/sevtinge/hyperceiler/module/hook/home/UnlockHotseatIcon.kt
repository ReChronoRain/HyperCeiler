package com.sevtinge.hyperceiler.module.hook.home

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.hookBeforeMethod

class UnlockHotseatIcon : BaseHook() {
    override fun init() {
        "com.miui.home.launcher.DeviceConfig".hookBeforeMethod("getHotseatMaxCount") {
            it.result = 99
        }
    }
}
