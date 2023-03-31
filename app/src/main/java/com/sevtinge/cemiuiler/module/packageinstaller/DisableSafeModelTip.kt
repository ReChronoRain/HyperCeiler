package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.builder.BatchFindArgs
import io.luckypray.dexkit.enums.MatchType
import java.util.*

class DisableSafeModelTip : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        val apkPath = lpparam.appInfo.sourceDir
        try {
            DexKitBridge.create(apkPath).use { bridge ->
                if (bridge == null) {
                    return
                }
                val resultMap =
                    bridge.batchFindMethodsUsingStrings(
                        BatchFindArgs.builder()
                            .addQuery("DisableSecurityModeFlag", listOf("user_close_security_mode_flag"))
                            .matchType(MatchType.CONTAINS)
                            .build()
                    )
                val result =
                    Objects.requireNonNull(resultMap["DisableSecurityModeFlag"])
                if (result != null) {
                    for (descriptor in result) {
                        val disableSecurityModeFlag = descriptor.getMethodInstance(lpparam.classLoader)
                        XposedBridge.log("Cemiuiler: DisableSafeModelTip disableSecurityModeFlag method is $disableSecurityModeFlag")
                        if (disableSecurityModeFlag.returnType == Boolean::class.javaPrimitiveType) {
                            XposedBridge.hookMethod(disableSecurityModeFlag, XC_MethodReplacement.returnConstant(true))
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        findAndHookMethod(
            "com.miui.packageInstaller.model.ApkInfo",
            "getSystemApp",
            XC_MethodReplacement.returnConstant(true)
        )
        hookAllMethods(
            "com.miui.packageInstaller.InstallProgressActivity",
            "g0",
            XC_MethodReplacement.returnConstant(false)
        )
        findAllMethods("com.miui.packageInstaller.InstallProgressActivity") { true }.hookAfter { param ->
            param.thisObject.javaClass.findField { type == Boolean::class.java }.setBoolean(param.thisObject, false)
        }

        //returnIntConstant(findClassIfExists("p6.a"), "d");
    }
}