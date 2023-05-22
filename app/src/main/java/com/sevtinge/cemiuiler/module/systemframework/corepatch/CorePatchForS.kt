package com.sevtinge.cemiuiler.module.systemframework.corepatch

import com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.InvocationTargetException


open class CorePatchForS : CorePatchForR() {
    @Throws(
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        super.handleLoadPackage(loadPackageParam)
        if (
            mPrefsMap.getBoolean("system_framework_core_patch_digest_creak") &&
            mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")
        ) {
            findAndHookMethod("com.android.server.pm.PackageManagerService",
                loadPackageParam.classLoader,
                "doesSignatureMatchForPermissions",
                String::class.java,
                "com.android.server.pm.parsing.pkg.ParsedPackage",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                        if (param.result == false) {
                            val pName =
                                XposedHelpers.callMethod(param.args[1], "getPackageName") as String
                            if (pName.contentEquals(param.args[0] as String)) {
                                param.result = true
                            }
                        }
                    }
                }
            )
        }
    }
}