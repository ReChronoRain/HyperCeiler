package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockSuperResolution : BaseHook() {
    override fun init() {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("ro.vendor.media.video.frc.support")
            }
        }.map {
            val qaq = it.getInstance(EzXHelper.classLoader)
            var counter = 0
            dexKitBridge.findMethod {
                matcher {
                    declaredClass = qaq.name
                    returnType = "boolean"
                    paramTypes = listOf("java.lang.String")
                }
            }.forEach { methods ->
                counter++
                if (counter == 1) {
                    methods.getMethodInstance(EzXHelper.classLoader).createHook {
                        returnConstant(true)
                    }
                }
            }
            dexKitBridge.findMethod {
                matcher {
                    declaredClass = qaq.name
                    usingStrings = listOf("debug.config.media.video.ais.support")
                }
            }.first().getMethodInstance(EzXHelper.classLoader).createHook {
                returnConstant(true)
            }
        }
    }
}
