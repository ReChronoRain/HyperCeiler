package com.sevtinge.hyperceiler.module.hook.systemui.base.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils
import com.sevtinge.hyperceiler.module.base.tool.HookTool

object Keyguard {
    // 锁屏底部左侧按钮
    @JvmStatic
    val leftButtonType by lazy {
        HookTool.mPrefsMap.getStringAsInt("system_ui_lock_screen_bottom_left_button", 0)
    }

    val keyguardBottomAreaInjector by lazy {
        ClassUtils.loadClass("com.android.keyguard.injector.KeyguardBottomAreaInjector")
    }
}