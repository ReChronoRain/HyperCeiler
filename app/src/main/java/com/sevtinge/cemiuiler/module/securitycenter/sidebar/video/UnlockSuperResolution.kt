package com.sevtinge.cemiuiler.module.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit
import com.sevtinge.cemiuiler.utils.DexKit
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import java.util.Objects

object UnlockSuperResolution : BaseHook() {
    override fun init() {
        DexKit.hostDir = lpparam.appInfo.sourceDir
        DexKit.loadDexKit()
        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultClassMap["FrcSupport"]
            )
            for (descriptor in result) {
                val frcSupport = descriptor.getClassInstance(lpparam.classLoader)
                log("frcSupport class is $frcSupport")
                var counter = 0
                DexKit.dexKitBridge.findMethod {
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
                log("aisSupport method is $aisSupport")
                if (aisSupport.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(aisSupport, XC_MethodReplacement.returnConstant(false))
                }
            }
        } catch (e: Throwable) {
            logE("AisSupport", e)
        }
    }
}
