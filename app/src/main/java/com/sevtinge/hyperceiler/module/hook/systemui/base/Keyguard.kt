package com.sevtinge.hyperceiler.module.hook.systemui.base

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass

object Keyguard {
    val keyguardBottomAreaInjector by lazy {
        loadClass("com.android.keyguard.injector.KeyguardBottomAreaInjector")
    }
}
