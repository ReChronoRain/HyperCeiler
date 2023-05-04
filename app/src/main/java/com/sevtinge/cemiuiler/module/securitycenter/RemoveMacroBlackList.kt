package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import java.lang.reflect.Method

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        val macro = SecurityCenterDexKit.mSecurityCenterResultClassMap["Macro"]!!
        assert(macro.size == 1)
        val macroDescriptor = macro.first()
        val macroClass: Class<*> = macroDescriptor.getClassInstance(lpparam.classLoader)
        findMethod(macroClass) {
            returnType == Boolean::class.java && parameterCount == 1
        }.hookReturnConstant(false)

        val macro1 = SecurityCenterDexKit.mSecurityCenterResultClassMap["Macro1"]!!
        assert(macro1.size == 1)
        val macro1Descriptor = macro1.first()
        val macro1Class: Class<*> = macro1Descriptor.getClassInstance(lpparam.classLoader)
        findMethod(macro1Class) {
            returnType == Boolean::class.java && parameterCount == 2
        }.hookReturnConstant(true)

        val macro2 = SecurityCenterDexKit.mSecurityCenterResultMap["Macro2"]!!
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