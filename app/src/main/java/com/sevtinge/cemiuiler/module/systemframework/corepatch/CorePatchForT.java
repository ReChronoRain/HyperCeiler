package com.sevtinge.cemiuiler.module.systemframework.corepatch;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;

public class CorePatchForT extends CorePatchForSv2 {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        XposedBridge.log("Cemiuiler: CorePatch Downgrade=" + mPrefsMap.getBoolean("system_framework_core_patch_downgr"));
        XposedBridge.log("Cemiuiler: CorePatch AuthCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_auth_creak"));
        XposedBridge.log("Cemiuiler: CorePatch DigestCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_digest_creak"));
        XposedBridge.log("Cemiuiler: CorePatch UsePreSig=" + mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature"));
        XposedBridge.log("Cemiuiler: CorePatch EnhancedMode=" + mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode"));

        findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader,
                "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "system_framework_core_patch_downgr", null));

        Class<?> signingDetails = getSigningDetails(loadPackageParam.classLoader);
        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (((Integer) param.args[1] != 4) && mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    param.setResult(true);
                }
            }
        });

        // Package " + packageName + " signatures do not match previously installed version; ignoring!"
        // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
        hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Log.e("CorePatch", "checkCapability");
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            }
        });


        if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak") && mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
            findAndHookMethod("com.android.server.pm.InstallPackageHelper", loadPackageParam.classLoader, "doesSignatureMatchForPermissions", String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (param.getResult().equals(false)) {
                        String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                        if (pPname.contentEquals((String) param.args[0])) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
        findAndHookMethod("com.android.server.pm.ScanPackageUtils", loadPackageParam.classLoader, "assertMinSignatureSchemeIsValid", "com.android.server.pm.parsing.pkg.AndroidPackage", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(null);
                }
            }
        });
        Class<?> strictJarVerifier = findClass("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader);
        if (strictJarVerifier != null) {
            XposedBridge.hookAllConstructors(strictJarVerifier, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                        XposedHelpers.setBooleanField(param.thisObject, "signatureSchemeRollbackProtectionsEnforced", false);
                    }
                }
            });
        }
    }

    @Override
    Class<?> getSigningDetails(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("android.content.pm.SigningDetails", classLoader);
    }
}

/*public class CorePatchForT extends CorePatchForSv2 {
    @Override
    public void init() {
        super.init();

        findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils",
                "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                XC_MethodReplacement.returnConstant(null));


        Class<?> signingDetails = findClassIfExists("android.content.pm.SigningDetails");
        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (((Integer) param.args[1] != 4) && mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    param.setResult(true);
                }
            }
        });

        // Package " + packageName + " signatures do not match previously installed version; ignoring!"
        // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
        hookAllMethods("android.content.pm.PackageParser", "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Log.e("CorePatch", "checkCapability");
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            }
        });


        if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak") && mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
            findAndHookMethod("com.android.server.pm.InstallPackageHelper", "doesSignatureMatchForPermissions", String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (param.getResult().equals(false)) {
                        String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                        if (pPname.contentEquals((String) param.args[0])) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
        findAndHookMethod("com.android.server.pm.ScanPackageUtils",  "assertMinSignatureSchemeIsValid", "com.android.server.pm.parsing.pkg.AndroidPackage", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(null);
                }
            }
        });
        Class<?> strictJarVerifier = findClass("android.util.jar.StrictJarVerifier" );
        if (strictJarVerifier != null) {
            XposedBridge.hookAllConstructors(strictJarVerifier, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                        XposedHelpers.setBooleanField(param.thisObject, "signatureSchemeRollbackProtectionsEnforced", false);
                    }
                }
            });
        }
    }


}*/