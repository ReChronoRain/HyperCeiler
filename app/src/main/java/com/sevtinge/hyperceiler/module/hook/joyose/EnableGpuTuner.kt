package com.sevtinge.hyperceiler.module.hook.joyose

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge


object EnableGpuTuner : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("GPUTUNER_SWITCH")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(true)
        }
    }
}
