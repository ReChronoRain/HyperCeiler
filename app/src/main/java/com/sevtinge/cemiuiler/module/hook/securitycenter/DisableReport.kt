package com.sevtinge.cemiuiler.module.hook.securitycenter

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object DisableReport : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.intent.action.VIEW", "com.xiaomi.market")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        /* val result: List<DexMethodDescriptor> =
             java.util.Objects.requireNonNull<List<DexMethodDescriptor>>(
                 SecurityCenterDexKit.mSecurityCenterResultMap["IsShowReport"]
             )
         for (descriptor in result) {
             val isShowReport: java.lang.reflect.Method =
                 descriptor.getMethodInstance(lpparam.classLoader)
             log("isShowReport method is $isShowReport")
             if (isShowReport.returnType == Boolean::class.javaPrimitiveType) {
                 XposedBridge.hookMethod(
                     isShowReport,
                     XC_MethodReplacement.returnConstant(false)
                 )
             }
         }*/
    }
}
