package com.sevtinge.hyperceiler.module.hook.milink

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockHMind : BaseHook() {
    override fun init() {
        val qaq = dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("habitInfoList")
                }
                addUsingStringsEquals("isHMindAble() isSupport: ")
                modifiers = Modifier.FINAL
                returnType = "boolean"
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()

        try {
            qaq.createHooks {
                after {
                    it.result = true
                }
            }
        } catch (t: Throwable) {
            logE("UnlockHMind Hook failed, find is $qaq")
        }
    }
}
