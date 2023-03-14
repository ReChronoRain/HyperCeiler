package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->

            val classMap = mapOf(
                "Macro" to setOf("pref_gb_unsupport_macro_apps", "gb_game_gunsight", "com.tencent.tmgp.sgame"),
                "Macro1" to setOf("key_macro_toast", "content://com.xiaomi.macro.MacroStatusProvider/game_macro_change"),
            )

            val methodMap = mapOf(
                "Macro2" to setOf("pref_gb_unsupport_macro_apps"),
            )

            val resultClassMap = bridge.batchFindClassesUsingStrings {
                queryMap(classMap)
            }

            val resultMethodMap = bridge.batchFindMethodsUsingStrings {
                queryMap(methodMap)
            }

            val macro = resultClassMap["Macro"]!!
            assert(macro.size == 1)
            val macroDescriptor = macro.first()
            val macroClass: Class<*> = macroDescriptor.getClassInstance(lpparam.classLoader)
            findMethod(macroClass) {
                returnType == Boolean::class.java && parameterCount == 1
            }.hookReturnConstant(false)

            val macro1 = resultClassMap["Macro1"]!!
            assert(macro1.size == 1)
            val macro1Descriptor = macro1.first()
            val macro1Class: Class<*> = macro1Descriptor.getClassInstance(lpparam.classLoader)
            findMethod(macro1Class) {
                returnType == Boolean::class.java && parameterCount == 2
            }.hookReturnConstant(true)

            val macro2 = resultMethodMap["Macro2"]!!
            assert(macro2.isNotEmpty())
            var macro2Descriptor = macro2[0]
            var macroMethod: Method = macro2Descriptor.getMethodInstance(lpparam.classLoader)
            if (macroMethod.returnType != ArrayList::class.java) {
                macro2Descriptor = macro2[1]
                macroMethod = macro2Descriptor.getMethodInstance(lpparam.classLoader)
            }
            macroMethod.hookBefore {
                it.result = ArrayList<String>()
            }

        }
    }
}