package com.sevtinge.hyperceiler.module.hook.securitycenter

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object DisableReport : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.intent.action.VIEW", "com.xiaomi.market")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }
    }
}
