package com.sevtinge.hyperceiler.module.hook.home.title

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.IS_TABLET

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
