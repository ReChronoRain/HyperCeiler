package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedModuleInterface;

public class DigestCreakPatch extends CorePatchHelper {

    private final String TAG = "DigestCreak/UsePreSignPatch";

    public void init(XposedModuleInterface.SystemServerLoadedParam lpparam) {
        // Android 14+
        try {
            findAndHookMethod("com.android.server.pm.InstallPackageHelper", lpparam.getClassLoader(),
                "doesSignatureMatchForPermissions", String.class,
                "com.android.internal.pm.parsing.pkg.ParsedPackage", int.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                            //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                            if (param.getResult().equals(false)) {
                                String pPname = (String) EzxHelpUtils.callMethod(param.getArgs()[1], "getPackageName");
                                if (pPname.contentEquals((String) param.getArgs()[0])) {
                                    param.setResult(true);
                                }
                            }
                        }
                    }
                });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 14+ hook failed, crash: " + t);
        }

        // Android 13+
        try {
            Class<?> signingDetails = getSigningDetails(lpparam.getClassLoader());
            // New package has a different signature
            // 处理覆盖安装但签名不一致
            hookAllMethods(signingDetails, "checkCapability", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    // Don't handle PERMISSION & AUTH
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/accounts/AccountManagerService.java;l=5867
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                        if ((Integer) param.getArgs()[1] != 4 && (Integer) param.getArgs()[1] != 16) {
                            param.setResult(true);
                        }
                    }
                }
            });

            Class<?> ParsedPackage = getParsedPackage(lpparam.getClassLoader());
            findAndHookMethod("com.android.server.pm.InstallPackageHelper", lpparam.getClassLoader(),
                "doesSignatureMatchForPermissions", String.class,
                ParsedPackage, int.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                            //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                            if (param.getResult().equals(false)) {
                                String pPname = (String) EzxHelpUtils.callMethod(param.getArgs()[1], "getPackageName");
                                if (pPname.contentEquals((String) param.getArgs()[0])) {
                                    param.setResult(true);
                                }
                            }
                        }
                    }
                });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 13+ hook failed, crash: " + t);
        }

        // Android 12+
        try {
            if (isAndroidVersion(31) || isAndroidVersion(32)) {
                var pmService = findClassIfExists("com.android.server.pm.PackageManagerService",
                    lpparam.getClassLoader());
                if (pmService != null) {
                    var doesSignatureMatchForPermissions = EzxHelpUtils.findMethodExactIfExists(pmService, "doesSignatureMatchForPermissions",
                        String.class, "com.android.internal.pm.parsing.pkg.ParsedPackage", int.class);
                    if (doesSignatureMatchForPermissions != null) {
                        hookMethod(doesSignatureMatchForPermissions, new IMethodHook() {
                            @Override
                            public void after(AfterHookParam param) {
                                if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                                    if (param.getResult().equals(false)) {
                                        String pPname = (String) EzxHelpUtils.callMethod(param.getArgs()[1], "getPackageName");
                                        if (pPname.contentEquals((String) param.getArgs()[0])) {
                                            param.setResult(true);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 12+ hook failed, crash: " + t);
        }

        // Android 11+ 1/2
        try {
            // 当 verifyV1Signature 抛出转换异常时，替换一个签名作为返回值
            // 如果用户已安装 apk，并且其定义了私有权限，则安装时会因签名与模块内硬编码的不一致而被拒绝。尝试从待安装apk中获取签名。如果其中apk的签名和已安装的一致（只动了内容）就没有问题。此策略可能有潜在的安全隐患。
            Class<?> pkc = findClass("sun.security.pkcs.PKCS7", lpparam.getClassLoader());
            Constructor<?> constructor = EzxHelpUtils.findConstructorExact(pkc, byte[].class);
            constructor.setAccessible(true);
            Class<?> ASV = findClass("android.util.apk.ApkSignatureVerifier", lpparam.getClassLoader());
            Class<?> sJarClass = findClass("android.util.jar.StrictJarFile", lpparam.getClassLoader());
            Constructor<?> constructorExact = EzxHelpUtils.findConstructorExact(sJarClass, String.class, boolean.class, boolean.class);
            constructorExact.setAccessible(true);
            Class<?> signingDetails = getSigningDetails(lpparam.getClassLoader());
            Constructor<?> findConstructorExact = EzxHelpUtils.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
            findConstructorExact.setAccessible(true);
            Class<?> packageParserException = findClass("android.content.pm.PackageParser.PackageParserException", lpparam.getClassLoader());
            Field error = EzxHelpUtils.findField(packageParserException, "error");
            error.setAccessible(true);
            Object[] signingDetailsArgs = new Object[2];
            signingDetailsArgs[1] = 1;
            Class<?> parseResult = findClassIfExists("android.content.pm.parsing.result.ParseResult", lpparam.getClassLoader());
            hookAllMethods("android.util.jar.StrictJarVerifier", lpparam.getClassLoader(), "verifyBytes", new IMethodHook() {
                public void after(AfterHookParam param) throws InvocationTargetException, IllegalAccessException, InstantiationException {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                        if (!prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                            final Object block = constructor.newInstance(param.getArgs()[0]);
                            Object[] infos = (Object[]) EzxHelpUtils.callMethod(block, "getSignerInfos");
                            Object info = infos[0];
                            @SuppressWarnings("unchecked")
                            List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) EzxHelpUtils.callMethod(info, "getCertificateChain", block);
                            param.setResult(verifiedSignerCertChain.toArray(
                                new X509Certificate[0]));
                        }
                    }
                }
            });
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
            // New package has a different signature
            // 处理覆盖安装但签名不一致
            hookAllMethods(signingDetails, "checkCapability", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    // Don't handle PERMISSION & AUTH
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/accounts/AccountManagerService.java;l=5867
                    if ((Integer) param.getArgs()[1] != 4 && (Integer) param.getArgs()[1] != 16 && prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                        param.setResult(true);
                    }
                }
            });
            // if app is system app, allow to use hidden api, even if app not using a system signature
            findAndHookMethod("android.content.pm.ApplicationInfo", lpparam.getClassLoader(), "isPackageWhitelistedForHiddenApis", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                        ApplicationInfo info = (ApplicationInfo) param.getThisObject();
                        if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                            param.setResult(true);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ 1/2 hook failed, crash: " + t);
        }

        // Android 11+ 2/2
        try {
            var keySetManagerClass = findClass("com.android.server.pm.KeySetManagerService", lpparam.getClassLoader());
            if (keySetManagerClass != null) {
                var shouldBypass = new ThreadLocal<Boolean>();
                hookAllMethods(keySetManagerClass, "shouldCheckUpgradeKeySetLocked", new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        // 检查权限定义的签名的时候，如果定义包名相同，会使用 KeySetManagerService
                        // 我们利用这一点让它通过检查，也就是同包不同签名权限可覆盖
                        // R-Sv2: PackageManagerService#preparePackageLI
                        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r21:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=17188;drc=960ffca13a519b0fb9e0942665577c62f97d0eea
                        // T-V: InstallPackageHelper#preparePackageLI
                        // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/InstallPackageHelper.java;l=1097;drc=5ea7e53c3a787e25af86b0f31933ddd68ae3514e
                        // 16: InstallPackageHelper#preparePackage
                        // https://cs.android.com/android/platform/superproject/+/android-16.0.0_r2:frameworks/base/services/core/java/com/android/server/pm/InstallPackageHelper.java;l=1459;drc=d14620262929e39a409b55d11cb542c1d1c4d2f6
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch((o) -> o.getMethodName().startsWith("preparePackage"))) {
                            shouldBypass.set(true);
                            param.setResult(true);
                        } else {
                            shouldBypass.set(false);
                        }
                    }
                });
                hookAllMethods(keySetManagerClass, "checkUpgradeKeySetLocked", new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && shouldBypass.get()) {
                            param.setResult(true);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ 2/2 hook failed, crash: " + t);
        }
    }

    Class<?> getParsedPackage(ClassLoader classLoader) {
        if (isMoreAndroidVersion(35)) {
            return findClassIfExists("com.android.internal.pm.parsing.pkg.ParsedPackage", classLoader);
        } else if (isMoreAndroidVersion(34)) {
            var clazz = findClassIfExists("com.android.internal.pm.parsing.pkg.ParsedPackage", classLoader);
            return clazz != null ? clazz : findClassIfExists("com.android.server.pm.parsing.pkg.ParsedPackage", classLoader);
        }
        return findClassIfExists("com.android.server.pm.parsing.pkg.ParsedPackage", classLoader);
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        if (isMoreAndroidVersion(33)) {
            return findClassIfExists("android.content.pm.SigningDetails", classLoader);
        }
        return findClass("android.content.pm.PackageParser.SigningDetails", classLoader);
    }
}
