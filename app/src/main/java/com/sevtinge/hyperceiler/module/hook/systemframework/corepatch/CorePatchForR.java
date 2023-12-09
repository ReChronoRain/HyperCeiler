package com.sevtinge.hyperceiler.module.hook.systemframework.corepatch;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForR extends XposedHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private final static Method deoptimizeMethod;
    private static final boolean isNotReleaseVersion = !BuildConfig.BUILD_TYPE.contains("release");
    public static final String TAG = "CorePatchForR";

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(t));
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Class<?> c, String n) throws InvocationTargetException, IllegalAccessException {
        for (Method m : c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals(n)) {
                deoptimizeMethod.invoke(null, m);
                if (isNotReleaseVersion)
                    XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": Deoptimized " + m.getName());
            }
        }
    }

    final XSharedPreferences prefs = new XSharedPreferences(Helpers.mAppModulePkg, PrefsUtils.mPrefsName);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (isNotReleaseVersion) {
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": downgrade=" + prefs.getBoolean("prefs_key_system_framework_core_patch_downgr", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": authcreak=" + prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": digestCreak=" + prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": UsePreSig=" + prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": enhancedMode=" + prefs.getBoolean("prefs_key_system_framework_core_patch_enhanced_mode", false));
        }

        var pmService = findClassIfExists("com.android.server.pm.PackageManagerService",
            loadPackageParam.classLoader);
        if (pmService != null) {
            var checkDowngrade = findMethodExactIfExists(pmService, "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite");
            if (checkDowngrade != null) {
                // 允许降级
                XposedBridge.hookMethod(checkDowngrade, new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", null));
            }
        }

        // apk内文件修改后 digest校验会失败
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyMessageDigest",
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", true));
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verify",
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", true));
        hookAllMethods("java.security.MessageDigest", loadPackageParam.classLoader, "isEqual",
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", true));

        // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
        // + " the resources.arsc of installed APKs to be stored uncompressed"
        // + " and aligned on a 4-byte boundary
        // target >=30 的情况下 resources.arsc 必须是未压缩的且4K对齐
        hookAllMethods("android.content.res.AssetManager", loadPackageParam.classLoader, "containsAllocatedTable",
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", false));

        // No signature found in package of version " + minSignatureSchemeVersion
        // + " or newer for package " + apkPath
        findAndHookMethod("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
            new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", 0));
        var apkVerifierClass = XposedHelpers.findClassIfExists("com.android.apksig.ApkVerifier",
            loadPackageParam.classLoader);
        if (apkVerifierClass != null) {
            findAndHookMethod(apkVerifierClass, "getMinimumSignatureSchemeVersionForTargetSdk", loadPackageParam.classLoader, int.class,
                new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", 0));
        }

        // Package " + packageName + " signatures do not match previously installed version; ignoring!"
        // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
        hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            }
        });

        // 当verifyV1Signature抛出转换异常时，替换一个签名作为返回值
        // 如果用户已安装apk，并且其定义了私有权限，则安装时会因签名与模块内硬编码的不一致而被拒绝。尝试从待安装apk中获取签名。如果其中apk的签名和已安装的一致（只动了内容）就没有问题。此策略可能有潜在的安全隐患。
        Class<?> pkc = XposedHelpers.findClass("sun.security.pkcs.PKCS7", loadPackageParam.classLoader);
        Constructor<?> constructor = XposedHelpers.findConstructorExact(pkc, byte[].class);
        constructor.setAccessible(true);
        Class<?> ASV = XposedHelpers.findClass("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader);
        Class<?> sJarClass = XposedHelpers.findClass("android.util.jar.StrictJarFile", loadPackageParam.classLoader);
        Constructor<?> constructorExact = XposedHelpers.findConstructorExact(sJarClass, String.class, boolean.class, boolean.class);
        constructorExact.setAccessible(true);
        Class<?> signingDetails = getSigningDetails(loadPackageParam.classLoader);
        Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
        findConstructorExact.setAccessible(true);
        Class<?> packageParserException = XposedHelpers.findClass("android.content.pm.PackageParser.PackageParserException", loadPackageParam.classLoader);
        Field error = XposedHelpers.findField(packageParserException, "error");
        error.setAccessible(true);
        Object[] signingDetailsArgs = new Object[2];
        signingDetailsArgs[1] = 1;
        Class<?> parseResult = findClassIfExists("android.content.pm.parsing.result.ParseResult", loadPackageParam.classLoader);
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyBytes", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                    if (!prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                        final Object block = constructor.newInstance(param.args[0]);
                        Object[] infos = (Object[]) XposedHelpers.callMethod(block, "getSignerInfos");
                        Object info = infos[0];
                        List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) XposedHelpers.callMethod(info, "getCertificateChain", block);
                        param.setResult(verifiedSignerCertChain.toArray(
                            new X509Certificate[0]));
                    }
                }
            }
        });
        hookAllMethods("android.util.apk.ApkSignatureVerifier", loadPackageParam.classLoader, "verifyV1Signature", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                    Throwable throwable = methodHookParam.getThrowable();
                    Integer parseErr = null;
                    if (parseResult != null && ((Method) methodHookParam.method).getReturnType() == parseResult) {
                        Object result = methodHookParam.getResult();
                        if ((boolean) XposedHelpers.callMethod(result, "isError")) {
                            parseErr = (int) XposedHelpers.callMethod(result, "getErrorCode");
                        }
                    }
                    if (throwable != null || parseErr != null) {
                        Signature[] lastSigs = null;
                        try {
                            if (prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false)) {
                                PackageManager PM = AndroidAppHelper.currentApplication().getPackageManager();
                                if (PM == null) {
                                    XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": [" + BuildConfig.APPLICATION_ID + "] Cannot get the Package Manager... Are you using MiUI?");
                                } else {
                                    PackageInfo pI;
                                    if (parseErr != null) {
                                        pI = PM.getPackageArchiveInfo((String) methodHookParam.args[1], 0);
                                    } else {
                                        pI = PM.getPackageArchiveInfo((String) methodHookParam.args[0], 0);
                                    }
                                    PackageInfo InstpI = PM.getPackageInfo(pI.packageName, PackageManager.GET_SIGNATURES);
                                    lastSigs = InstpI.signatures;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                        try {
                            if (lastSigs == null && prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                                final Object origJarFile = constructorExact.newInstance(methodHookParam.args[parseErr == null ? 0 : 1], true, false);
                                final ZipEntry manifestEntry = (ZipEntry) XposedHelpers.callMethod(origJarFile, "findEntry", "AndroidManifest.xml");
                                final Certificate[][] lastCerts;
                                if (parseErr != null) {
                                    lastCerts = (Certificate[][]) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(ASV, "loadCertificates", methodHookParam.args[0], origJarFile, manifestEntry), "getResult");
                                } else {
                                    lastCerts = (Certificate[][]) XposedHelpers.callStaticMethod(ASV, "loadCertificates", origJarFile, manifestEntry);
                                }
                                lastSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV, "convertToSignatures", (Object) lastCerts);
                            }
                        } catch (Throwable ignored) {
                        }
                        if (lastSigs != null) {
                            signingDetailsArgs[0] = lastSigs;
                        } else {
                            signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
                        }
                        Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);

                        // 修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                        Class<?> signingDetailsWithDigests = findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests", loadPackageParam.classLoader);
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> signingDetailsWithDigestsConstructorExact = XposedHelpers.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
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
                            Object input = methodHookParam.args[0];
                            XposedHelpers.callMethod(input, "reset");
                            methodHookParam.setResult(XposedHelpers.callMethod(input, "success", newInstance));
                        }
                    }
                }
            }
        });


        // New package has a different signature
        // 处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                if (((Integer) param.args[1] != 4) && prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                    param.setResult(true);
                }
            }
        });
        // if app is system app, allow to use hidden api, even if app not using a system signature
        findAndHookMethod("android.content.pm.ApplicationInfo", loadPackageParam.classLoader, "isPackageWhitelistedForHiddenApis", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                    ApplicationInfo info = (ApplicationInfo) param.thisObject;
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        param.setResult(true);
                    }
                }
            }
        });

        var utilClass = findClass("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader);
        if (utilClass != null) {
            try {
                deoptimizeMethod(utilClass, "verifySignatures");
            } catch (Throwable e) {
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": deoptimizing failed" + Log.getStackTraceString(e));
            }
        }

        var keySetManagerClass = findClass("com.android.server.pm.KeySetManagerService", loadPackageParam.classLoader);
        if (keySetManagerClass != null) {
            var shouldBypass = new ThreadLocal<Boolean>();
            hookAllMethods(keySetManagerClass, "shouldCheckUpgradeKeySetLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch((o) -> "preparePackageLI".equals(o.getMethodName()))) {
                        shouldBypass.set(true);
                        param.setResult(true);
                    } else {
                        shouldBypass.set(false);
                    }
                }
            });
            hookAllMethods(keySetManagerClass, "checkUpgradeKeySetLocked", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("digestCreak", true) && shouldBypass.get()) {
                        param.setResult(true);
                    }
                }
            });
        }
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        return XposedHelpers.findClass("android.content.pm.PackageParser.SigningDetails", classLoader);
    }

    @Override
    public void initZygote(StartupParam startupParam) {

        hookAllMethods("android.content.pm.PackageParser", null, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));
        hookAllConstructors("android.util.jar.StrictJarVerifier", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_enhanced_mode", false)) {
                    super.beforeHookedMethod(param);
                    param.args[3] = Boolean.FALSE;
                }
            }
        });
    }
}
