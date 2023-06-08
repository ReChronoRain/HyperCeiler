package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import java.lang.reflect.Method

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        val macro = SecurityCenterDexKit.mSecurityCenterResultClassMap["Macro"]!!
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
        }
    }
}