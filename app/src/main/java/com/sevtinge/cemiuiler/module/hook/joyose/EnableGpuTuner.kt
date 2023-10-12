package com.sevtinge.cemiuiler.module.hook.joyose

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge


object EnableGpuTuner : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("GPUTUNER_SWITCH")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(true)
        }
    }
}
