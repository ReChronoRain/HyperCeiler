package com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("pref_gb_unsupport_macro_apps")
                paramTypes = listOf("java.util.ArrayList")
                returnType = "void"
            }
        }.first().getMethodInstance(EzXHelper.classLoader).createHook {
            before {
                it.result = ArrayList<String>()
            }
        }

        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("com.netease.sky.mi")
            }
        }.first().getInstance(EzXHelper.classLoader).methodFinder()
            .filterByReturnType(Boolean::class.java)
            .filterByParamCount(1)
            .first().createHook {
                returnConstant(false)
            }

        dexKitBridge.findClass {
            matcher {
                usingStrings =
                    listOf("content://com.xiaomi.macro.MacroStatusProvider/game_macro_change")
            }
        }.first().getInstance(EzXHelper.classLoader).methodFinder()
            .filterByReturnType(Boolean::class.java)
            .filterByParamCount(2)
            .first().createHook {
                returnConstant(true)
            }

        /*val macro = SecurityCenterDexKit.mSecurityCenterResultClassMap["Macro"]!!
        assert(macro.size == 1)
        val macroDescriptor = macro.first()
        val macroClass: Class<*> = macroDescriptor.getClassInstance(lpparam.classLoader)
        macroClass.methodFinder().first {
            returnType == Boolean::class.java && parameterCount == 1
        }.createHook {
            returnConstant(false)
        }

        val macro1 = SecurityCenterDexKit.mSecurityCenterResultClassMap["Macro1"]!!
        assert(macro1.size == 1)
        val macro1Descriptor = macro1.first()
        val macro1Class: Class<*> = macro1Descriptor.getClassInstance(lpparam.classLoader)
        macro1Class.methodFinder().first {
            returnType == Boolean::class.java && parameterCount == 2
        }.createHook {
            returnConstant(true)
        }

        val macro2 = SecurityCenterDexKit.mSecurityCenterResultMap["Macro2"]!!
        assert(macro2.isNotEmpty())
        var macro2Descriptor = macro2[0]
        var macroMethod: Method = macro2Descriptor.getMethodInstance(lpparam.classLoader)
        if (macroMethod.returnType != ArrayList::class.java) {
            macro2Descriptor = macro2[1]
            macroMethod = macro2Descriptor.getMethodInstance(lpparam.classLoader)
        }
        macroMethod.createHook {
            before {
                it.result = ArrayList<String>()
            }
        }*/
    }
}
