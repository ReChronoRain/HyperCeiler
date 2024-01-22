package com.sevtinge.hyperceiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object DisableAD : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ads_enable")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("app_store_recommend")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("virus_scan_install")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }
    }
}
