package com.sevtinge.hyperceiler.module.hook.mishare

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockTurboMode : BaseHook() {
    private val turboModeMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("DeviceUtil", "xiaomi.hardware.p2p_160m")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    override fun init() {
        turboModeMethod.createHook {
            returnConstant(true)
        }
    }
}
