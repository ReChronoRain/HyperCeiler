package com.sevtinge.hyperceiler.module.hook.systemframework.corepatch;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForU extends CorePatchForT {
    public static final String TAG = "[CorePatchForU]";
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        var utilClass = findClass("com.android.server.pm.ReconcilePackageUtils", loadPackageParam.classLoader);
        if (utilClass != null) {
            try {
                deoptimizeMethod(utilClass, "reconcilePackages");
            } catch (Throwable e) {
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": deoptimizing failed" + Log.getStackTraceString(e));
            }
        }

        // ee11a9c (Rename AndroidPackageApi to AndroidPackage)
        findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader,
            "checkDowngrade",
            "com.android.server.pm.pkg.AndroidPackage",
            "android.content.pm.PackageInfoLite",
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", null));


        findAndHookMethod("com.android.server.pm.InstallPackageHelper", loadPackageParam.classLoader,
            "doesSignatureMatchForPermissions", String.class,
            "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                        //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                        if (param.getResult().equals(false)) {
                            String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                            if (pPname.contentEquals((String) param.args[0])) {
                                param.setResult(true);
                            }
                        }
                    }
                }
            });

        findAndHookMethod("com.android.server.pm.ScanPackageUtils", loadPackageParam.classLoader,
            "assertMinSignatureSchemeIsValid",
            "com.android.server.pm.pkg.AndroidPackage", int.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                        param.setResult(null);
                    }
                }
            });

    }

}
