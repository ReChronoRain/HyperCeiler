package com.sevtinge.hyperceiler.hook.module.hook.mishare

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
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
