package com.sevtinge.cemiuiler.module.systemframework.corepatch;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;
import static com.sevtinge.cemiuiler.utils.Helpers.log;

public class CorePatchForS extends CorePatchForR {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        log("CorePatchForS Downgrade=" + mPrefsMap.getBoolean("system_framework_core_patch_downgr"));
        log("CorePatchForS AuthCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_auth_creak"));
        log("CorePatchForS DigestCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_digest_creak"));
        log("CorePatchForS UsePreSig=" + mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature"));
        log("CorePatchForS EnhancedMode=" + mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode"));

        if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak") && mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
            findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader, "doesSignatureMatchForPermissions", String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (param.getResult().equals(false)) {
                        String pName = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                        if (pName.contentEquals((String) param.args[0])) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
    }
}