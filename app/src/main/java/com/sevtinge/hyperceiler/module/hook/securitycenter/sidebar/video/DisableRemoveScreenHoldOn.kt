package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object DisableRemoveScreenHoldOn : BaseHook() {
    private val screen by lazy {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("remove_screen_off_hold_on")
                returnType = "boolean"
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    override fun init() {
        screen.createHook {
            before {
                it.result = false
            }
        }
    }
}
