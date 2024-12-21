package com.sevtinge.hyperceiler.module.hook.mishare

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import java.lang.reflect.*

object NoAutoTurnOnLocation : BaseHook() {
    override fun init() {
        val location by lazy {
            DexKit.findMember<Method>("location") {
                it.findMethod {
                    searchPackages("com.miui.mishare.connectivity")
                    matcher {
                        usingEqStrings("setLocationEnabledForUser")
                        modifiers = Modifier.STATIC
                        returnType = "boolean"
                    }
                }.single()
            }
        }

        location.createHook {
            returnConstant(true)
        }
    }
}