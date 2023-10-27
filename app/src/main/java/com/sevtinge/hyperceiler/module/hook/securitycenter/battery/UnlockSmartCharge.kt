package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockSmartCharge : BaseHook() {
    private val smartChg by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("persist.vendor.smartchg")
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    override fun init() {
        smartChg.createHooks {
            returnConstant(true)
        }
    }
}
