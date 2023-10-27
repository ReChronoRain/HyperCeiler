package com.sevtinge.hyperceiler.module.hook.joyose

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object DisableCloudControl : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("job exist, sync local...")
                returnType = "void"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(null)
        }
    }
}
