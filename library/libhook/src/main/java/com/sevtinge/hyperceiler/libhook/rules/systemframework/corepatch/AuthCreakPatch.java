package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimizeMethods;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findMethodExactIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.io.RandomAccessFile;
import java.util.Arrays;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedModuleInterface;

public class AuthCreakPatch extends CorePatchHelper {
    private final String TAG = "AuthCreakPatch";

    public void init(XposedModuleInterface.SystemServerLoadedParam lpparam) {
        // Android 14+
        try {
            findAndHookMethod("com.android.server.pm.ScanPackageUtils", lpparam.getClassLoader(),
                "assertMinSignatureSchemeIsValid",
                "com.android.server.pm.pkg.AndroidPackage", int.class,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                            param.setResult(null);
                        }
                    }
                });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 14+ hook failed, crash: " + t);
        }

        // Android 13+
        try {
            if (isAndroidVersion(33)) {
                var assertMinSignatureSchemeIsValid = findMethodExactIfExists("com.android.server.pm.ScanPackageUtils", lpparam.getClassLoader(),
                    "assertMinSignatureSchemeIsValid",
                    "com.android.server.pm.parsing.pkg.AndroidPackage", int.class);
                if (assertMinSignatureSchemeIsValid != null) {
                    hookMethod(assertMinSignatureSchemeIsValid, new IMethodHook() {
                        @Override
                        public void after(AfterHookParam param) {
                            if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                                param.setResult(null);
                            }
                        }
                    });
                }
            }

            Class<?> strictJarVerifier = findClass("android.util.jar.StrictJarVerifier", lpparam.getClassLoader());
            if (strictJarVerifier != null) {
                hookAllConstructors(strictJarVerifier, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                            setBooleanField(param.getThisObject(), "signatureSchemeRollbackProtectionsEnforced", false);
                        }
                    }
                });
            }

            // ensure verifySignatures success
            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=621;drc=2e50991320cbef77d3e8504a4b284adae8c2f4d2
            var utils = findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", lpparam.getClassLoader());
            if (utils != null) {
                deoptimizeMethods(utils, "canJoinSharedUserId");
            }

            var apkSigningBlockClass = findClass("android.util.apk.ApkSigningBlockUtils", lpparam.getClassLoader());
            var signatureInfoClass = findClass("android.util.apk.SignatureInfo", lpparam.getClassLoader());
            findAndHookMethod(apkSigningBlockClass, "parseVerityDigestAndVerifySourceLength", byte[].class, long.class, signatureInfoClass, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", false)) {
                        param.setResult(Arrays.copyOfRange((byte[]) param.getArgs()[0], 0, 32));
                    }
                }
            });

            findAndHookMethod(apkSigningBlockClass, "verifyIntegrityForVerityBasedAlgorithm", byte[].class, RandomAccessFile.class, signatureInfoClass, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", false)) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 13+ hook failed, crash: " + t);
        }

        // Android 11+
        try {
            // apk内文件修改后 digest校验会失败
            hookAllMethods("android.util.jar.StrictJarVerifier", lpparam.getClassLoader(), "verifyMessageDigest",
                new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", true));
            hookAllMethods("android.util.jar.StrictJarVerifier", lpparam.getClassLoader(), "verify",
                new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", true));
            hookAllMethods("java.security.MessageDigest", lpparam.getClassLoader(), "isEqual",
                new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", true));

            // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
            // + " the resources.arsc of installed APKs to be stored uncompressed"
            // + " and aligned on a 4-byte boundary
            // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
            hookAllMethods("android.content.res.AssetManager", lpparam.getClassLoader(), "containsAllocatedTable",
                new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", false));

            // No signature found in package of version " + minSignatureSchemeVersion
            // + " or newer for package " + apkPath
            findAndHookMethod("android.util.apk.ApkSignatureVerifier", lpparam.getClassLoader(), "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", 0));
            var apkVerifierClass = findClassIfExists("com.android.apksig.ApkVerifier",
                lpparam.getClassLoader());
            if (apkVerifierClass != null) {
                findAndHookMethod(apkVerifierClass, "getMinimumSignatureSchemeVersionForTargetSdk", lpparam.getClassLoader(), int.class,
                    new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", 0));
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }
}
