package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimizeMethods;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findMethodExactIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;

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
                findAndHookMethod(apkVerifierClass, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                    new ReturnConstant("prefs_key_system_framework_core_patch_auth_creak", 0));
            }

            Class<?> sJarClass = findClass("android.util.jar.StrictJarFile", lpparam.getClassLoader());
            Constructor<?> constructorExact = EzxHelpUtils.findConstructorExact(sJarClass, String.class, boolean.class, boolean.class);
            constructorExact.setAccessible(true);
            Class<?> ASV = findClass("android.util.apk.ApkSignatureVerifier", lpparam.getClassLoader());
            Class<?> signingDetails = getSigningDetails(lpparam.getClassLoader());
            Constructor<?> findConstructorExact = EzxHelpUtils.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
            findConstructorExact.setAccessible(true);
            Class<?> packageParserException = findClass("android.content.pm.PackageParser.PackageParserException", lpparam.getClassLoader());
            Field error = EzxHelpUtils.findField(packageParserException, "error");
            error.setAccessible(true);
            Class<?> parseResult = findClassIfExists("android.content.pm.parsing.result.ParseResult", lpparam.getClassLoader());
            Object[] signingDetailsArgs = new Object[2];
            signingDetailsArgs[1] = 1;

            hookAllMethods("android.util.apk.ApkSignatureVerifier", lpparam.getClassLoader(), "verifyV1Signature", new IMethodHook() {
                public void after(AfterHookParam methodHookParam) throws InvocationTargetException, IllegalAccessException, InstantiationException {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                        Throwable throwable = methodHookParam.getThrowable();
                        Integer parseErr = null;
                        if (parseResult != null && ((Method) methodHookParam.getMember()).getReturnType() == parseResult) {
                            Object result = methodHookParam.getResult();
                            if ((boolean) EzxHelpUtils.callMethod(result, "isError")) {
                                parseErr = (int) EzxHelpUtils.callMethod(result, "getErrorCode");
                            }
                        }
                        if (throwable != null || parseErr != null) {
                            Signature[] lastSigs = null;
                            try {
                                if (prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                                    Class<?> activityThreadClazz =
                                        findClassIfExists("android.app.ActivityThread", lpparam.getClassLoader());
                                    Method currentApplicationMethod =
                                        activityThreadClazz.getDeclaredMethod("currentApplication");
                                    Application application =
                                        (Application) currentApplicationMethod.invoke(null);
                                    PackageManager PM = application.getPackageManager();
                                    if (PM == null) {
                                        XposedLog.w(TAG, "system", ProjectApi.mAppModulePkg + " Cannot get the Package Manager... Are you using MiUI?");
                                    } else {
                                        PackageInfo pI;
                                        if (parseErr != null) {
                                            pI = PM.getPackageArchiveInfo((String) methodHookParam.getArgs()[1], 0);
                                        } else {
                                            pI = PM.getPackageArchiveInfo((String) methodHookParam.getArgs()[0], 0);
                                        }
                                        PackageInfo InstpI = PM.getPackageInfo(pI.packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                                        lastSigs = InstpI.signingInfo.getSigningCertificateHistory();
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                            try {
                                if (lastSigs == null && prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                                    final Object origJarFile = constructorExact.newInstance(methodHookParam.getArgs()[parseErr == null ? 0 : 1], true, false);
                                    final ZipEntry manifestEntry = (ZipEntry) EzxHelpUtils.callMethod(origJarFile, "findEntry", "AndroidManifest.xml");
                                    final Certificate[][] lastCerts;
                                    if (parseErr != null) {
                                        lastCerts = (Certificate[][]) EzxHelpUtils.callMethod(EzxHelpUtils.callStaticMethod(ASV, "loadCertificates", methodHookParam.getArgs()[0], origJarFile, manifestEntry), "getResult");
                                    } else {
                                        lastCerts = (Certificate[][]) EzxHelpUtils.callStaticMethod(ASV, "loadCertificates", origJarFile, manifestEntry);
                                    }
                                    lastSigs = (Signature[]) EzxHelpUtils.callStaticMethod(ASV, "convertToSignatures", (Object) lastCerts);
                                }
                            } catch (Throwable ignored) {
                            }
                            signingDetailsArgs[0] = Objects.requireNonNullElseGet(lastSigs, () -> new Signature[]{new Signature(SIGNATURE)});
                            Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);

                            // 修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                            Class<?> signingDetailsWithDigests = findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests", lpparam.getClassLoader());
                            if (signingDetailsWithDigests != null) {
                                Constructor<?> signingDetailsWithDigestsConstructorExact = EzxHelpUtils.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
                                signingDetailsWithDigestsConstructorExact.setAccessible(true);
                                newInstance = signingDetailsWithDigestsConstructorExact.newInstance(newInstance, null);
                            }
                            if (throwable != null) {
                                Throwable cause = throwable.getCause();
                                if (throwable.getClass() == packageParserException) {
                                    if (error.getInt(throwable) == -103) {
                                        methodHookParam.setResult(newInstance);
                                    }
                                }
                                if (cause != null && cause.getClass() == packageParserException) {
                                    if (error.getInt(cause) == -103) {
                                        methodHookParam.setResult(newInstance);
                                    }
                                }
                            }
                            if (parseErr != null && parseErr == -103) {
                                Object input = methodHookParam.getArgs()[0];
                                EzxHelpUtils.callMethod(input, "reset");
                                methodHookParam.setResult(EzxHelpUtils.callMethod(input, "success", newInstance));
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        if (isMoreAndroidVersion(33)) {
            return findClassIfExists("android.content.pm.SigningDetails", classLoader);
        }
        return findClass("android.content.pm.PackageParser.SigningDetails", classLoader);
    }
}
