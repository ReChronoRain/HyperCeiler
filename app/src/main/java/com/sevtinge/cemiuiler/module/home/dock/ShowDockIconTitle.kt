package com.sevtinge.cemiuiler.module.home.dock

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.hookBeforeMethod

object ShowDockIconTitle : BaseHook() {
    override fun init() {

        "com.miui.home.launcher.DeviceConfig".hookBeforeMethod("isHotseatsAppTitleHided") {
            it.result = false
        }

    }
}