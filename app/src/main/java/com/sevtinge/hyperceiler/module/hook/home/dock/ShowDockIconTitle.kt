package com.sevtinge.hyperceiler.module.hook.home.dock

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.hookBeforeMethod

object ShowDockIconTitle : BaseHook() {
    override fun init() {

        "com.miui.home.launcher.DeviceConfig".hookBeforeMethod("isHotseatsAppTitleHided") {
            it.result = false
        }

    }
}
