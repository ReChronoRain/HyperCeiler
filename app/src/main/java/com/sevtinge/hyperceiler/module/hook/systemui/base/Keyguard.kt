package com.sevtinge.hyperceiler.module.hook.systemui.base

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.module.base.tool.HookTool.mPrefsMap

object Keyguard {
    // 锁屏底部左侧按钮
    @JvmStatic
    val leftButtonType by lazy {
        mPrefsMap.getStringAsInt("system_ui_lock_screen_bottom_left_button", 0)
    }

    val keyguardBottomAreaInjector by lazy {
        loadClass("com.android.keyguard.injector.KeyguardBottomAreaInjector")
    }
}
