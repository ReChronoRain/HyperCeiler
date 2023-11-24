package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockLeicaFilter : BaseHook() {
    override fun init() {
        val leica1 = dexKitBridge.findMethod {
            matcher {
                // 1.5 找不到，遂放弃
                /*addCall {
                    addCall {
                        addUsingStringsEquals("context.getString(R.string.filter_category_leica)")
                        returnType = "java.util.List"
                        modifiers = Modifier.FINAL
                        // paramCount = 0
                    }
                    returnType = "boolean"
                }*/
                addCall {
                    addCall {
                        addUsingStringsEquals("wayne")
                        returnType = "java.util.List"
                        modifiers = Modifier.STATIC
                    }
                    returnType = "boolean"
                }
                returnType = "boolean"
                modifiers = Modifier.STATIC
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()

        val leica2 = dexKitBridge.findMethod {
            matcher {
                // 仅适配 1.6.0.0.5
                declaredClass {
                    addUsingStringsEquals("unSupportDeviceList", "stringResUrl")
                }
                modifiers = Modifier.FINAL
                returnType = "boolean"
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()

        // debug 用
        for (l in leica1) {
            logI(TAG, "LeicaA name is $l")
        }
        for (l in leica2) {
            logI(TAG,"LeicaB name is $l")
        }

        try {
            leica1.createHooks {
                returnConstant(true)
            }
            leica2.createHooks {
                returnConstant(true)
            }
        } catch (t: Throwable) {
            logE(TAG, "no find leica method")
            return
        }
    }
}
