package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockGunService : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    usingStrings = listOf("gb_game_collimator_status")
                }
                returnType = "boolean"
                paramTypes = listOf("java.lang.String")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader).createHook {
            returnConstant(true)
        }
    }
}
