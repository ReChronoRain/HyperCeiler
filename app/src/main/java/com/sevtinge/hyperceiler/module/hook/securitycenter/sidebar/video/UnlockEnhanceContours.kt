package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockEnhanceContours : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("ro.vendor.media.video.frc.support")
            }
        }.forEach {
            val qaq = it.getClassInstance(EzXHelper.classLoader)
            var counter = 0
            dexKitBridge.findMethod {
                matcher {
                    declaredClass = qaq.name
                    returnType = "boolean"
                    paramTypes = listOf("java.lang.String")
                }
            }.forEach { methods ->
                counter++
                if (counter == 3) {
                    methods.getMethodInstance(EzXHelper.classLoader).createHook {
                        returnConstant(true)
                    }
                }
            }
            val tat = dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("debug.config.media.video.ais.support")
                    declaredClass = qaq.name
                }
            }.first().getMethodInstance(EzXHelper.classLoader)
            val newChar = tat.name.toCharArray()
            for (i in newChar.indices) {
                newChar[i]++
            }
            val newName = String(newChar)
            tat.declaringClass.methodFinder()
                .filterByName(newName)
                .first().createHook {
                    returnConstant(true)
                }
        }

        /*initDexKit(lpparam)
        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultClassMap["FrcSupport"]
            )
            for (descriptor in result) {
                val frcSupport = descriptor.getClassInstance(lpparam.classLoader)
                logI("frcSupport class is $frcSupport")
                var counter = 0
                dexKitBridge.findMethod {
                    methodDeclareClass = frcSupport.name
                    methodReturnType = "boolean"
                    methodParamTypes = arrayOf("java.lang.String")
                }.forEach { methods ->
                    counter++
                    if (counter == 3) {
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
                logI("aisSupport method is $aisSupport")
                val newChar = aisSupport.name.toCharArray()
                for (i in newChar.indices) {
                    newChar[i]++
                }
                val newName = String(newChar)
                aisSupport.declaringClass.methodFinder()
                    .filterByName(newName)
                    .first().createHook {
                        returnConstant(true)
                    }
            }
        } catch (e: Throwable) {
            logE("AisSupport", e)
        }
        closeDexKit()*/
    }
}
