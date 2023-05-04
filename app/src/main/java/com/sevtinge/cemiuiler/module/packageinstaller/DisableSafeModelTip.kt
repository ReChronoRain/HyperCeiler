package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.packageinstaller.PackageInstallerDexKit.mPackageInstallerResultMethodsMap
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import java.util.*

class DisableSafeModelTip : BaseHook() {
    override fun init() {
        try {
            val result =
                Objects.requireNonNull(mPackageInstallerResultMethodsMap["DisableSecurityModeFlag"])
            for (descriptor in result) {
                val disableSecurityModeFlag = descriptor.getMethodInstance(lpparam.classLoader)
                XposedBridge.log("Cemiuiler: DisableSafeModelTip disableSecurityModeFlag method is $disableSecurityModeFlag")
                if (disableSecurityModeFlag.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(disableSecurityModeFlag, XC_MethodReplacement.returnConstant(true))
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