package com.sevtinge.cemiuiler.module.hook.joyose

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType


object EnableGpuTuner : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("GPUTUNER_SWITCH")
                returnType = "boolean"
            }
        }.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                returnConstant(true)
            }
        }
    }
}
