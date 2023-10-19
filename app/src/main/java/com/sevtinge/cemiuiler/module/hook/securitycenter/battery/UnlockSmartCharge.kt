package com.sevtinge.cemiuiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

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
