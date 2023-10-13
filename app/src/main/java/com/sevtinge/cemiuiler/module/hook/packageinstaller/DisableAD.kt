package com.sevtinge.cemiuiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object DisableAD : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ads_enable")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("app_store_recommend")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("virus_scan_install")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }
    }
}
