package com.sevtinge.cemiuiler.module.home.title

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.IS_TABLET

object DisableHideTheme: BaseHook() {
    override fun init() {
        if (!IS_TABLET) return

        hookAllMethods("com.miui.home.launcher.DeviceConfig", "needHideThemeManager",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = false
                }
            }
        )
    }
}
