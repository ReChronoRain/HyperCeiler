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

       /* initDexKit(lpparam)
        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultClassMap["FrcSupport"]
            )
            for (descriptor in result) {
                val frcSupport = descriptor.getClassInstance(lpparam.classLoader)
                XposedLogUtils.logI("frcSupport class is $frcSupport")
                var counter = 0
                dexKitBridge.findMethod {
                    methodDeclareClass = frcSupport.name
                    methodReturnType = "boolean"
                    methodParamTypes = arrayOf("java.lang.String")
                }.forEach { methods ->
                    counter++
                    if (counter == 1) {
                        methods.getMethodInstance(EzXHelper.classLoader).createHook {
                            returnConstant(true)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logE("FrcSupport", e)
        }
        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultMap["AisSupport"]
            )
            for (descriptor in result) {
                val aisSupport = descriptor.getMethodInstance(lpparam.classLoader)
                XposedLogUtils.logI("aisSupport method is $aisSupport")
                if (aisSupport.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(aisSupport, XC_MethodReplacement.returnConstant(true))
                }
            }
        } catch (e: Throwable) {
            logE("AisSupport", e)
        }
        closeDexKit()*/
    }
}
