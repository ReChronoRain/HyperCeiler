package com.sevtinge.cemiuiler.module.hook.home.dock

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookBeforeMethod

object ShowDockIconTitle : BaseHook() {
    override fun init() {

        "com.miui.home.launcher.DeviceConfig".hookBeforeMethod("isHotseatsAppTitleHided") {
            it.result = false
        }

    }
}
