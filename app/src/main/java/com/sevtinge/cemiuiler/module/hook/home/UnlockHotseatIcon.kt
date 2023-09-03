package com.sevtinge.cemiuiler.module.hook.home

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookBeforeMethod

class UnlockHotseatIcon : BaseHook() {
    override fun init() {
        "com.miui.home.launcher.DeviceConfig".hookBeforeMethod("getHotseatMaxCount") {
            it.result = 99
        }
    }
}
