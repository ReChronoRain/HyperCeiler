package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockGunService : BaseHook() {
    override fun init() {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("gb_game_collimator_status")
            }
        }.map {
            val gbGameCollimatorClass = it.getInstance(EzXHelper.classLoader)
            dexKitBridge.findMethod {
                matcher {
                    declaredClass = gbGameCollimatorClass.name
                    returnType = "boolean"
                    paramTypes = listOf("java.lang.String")
                }
            }.single().getMethodInstance(EzXHelper.classLoader).createHook {
                returnConstant(true)
            }
        }

        /*val gbGameCollimator = mSecurityCenterResultClassMap["GbGameCollimator"]!!
        assert(gbGameCollimator.size == 1)
        val gbGameCollimatorDescriptor = gbGameCollimator.first()
        val gbGameCollimatorClass: Class<*> = gbGameCollimatorDescriptor.getClassInstance(lpparam.classLoader)
        gbGameCollimatorClass.methodFinder().first {
            returnType == Boolean::class.java && parameterCount == 1
        }.createHook {
            logI("GunService class is $gbGameCollimatorClass")
            returnConstant(true)
        }*/
    }
}
