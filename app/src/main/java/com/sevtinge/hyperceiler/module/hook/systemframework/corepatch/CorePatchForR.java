/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemframework.corepatch;

import static com.sevtinge.hyperceiler.BuildConfig.DEBUG;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.io.PrintWriter;
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
import java.util.Objects;
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
    private static final boolean isNotReleaseVersion = !ProjectApi.isRelease();
    public static final String TAG = "[CorePatchForR]";

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

    final XSharedPreferences prefs = new XSharedPreferences(ProjectApi.mAppModulePkg, PrefsUtils.mPrefsName);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (isNotReleaseVersion) {
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": downgrade=" + prefs.getBoolean("prefs_key_system_framework_core_patch_downgr", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": authcreak=" + prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": digestCreak=" + prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": UsePreSig=" + prefs.getBoolean("prefs_key_system_framework_core_patch_use_pre_signature", false));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": enhancedMode=" + prefs.getBoolean("prefs_key_system_framework_core_patch_enhanced_mode", false));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + " sharedUser=" + prefs.getBoolean("prefs_key_system_framework_core_patch_shared_user", false));
            XposedBridge.log("[HyperCeiler][D][android]" + TAG + "disableVerificationAgent=" + prefs.getBoolean("prefs_key_system_framework_disable_verification_agent", true));
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
                                    XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": [" + ProjectApi.mAppModulePkg + "] Cannot get the Package Manager... Are you using MiUI?");
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
                        signingDetailsArgs[0] = Objects.requireNonNullElseGet(lastSigs, () -> new Signature[]{new Signature(SIGNATURE)});
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
                // Don't handle PERMISSION & AUTH
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/accounts/AccountManagerService.java;l=5867
                if ((Integer) param.args[1] != 4 && (Integer) param.args[1] != 16 && prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
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
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) && shouldBypass.get()) {
                        param.setResult(true);
                    }
                }
            });
        }

        // for SharedUser
        // "Package " + packageName + " has a signing lineage " + "that diverges from the lineage of the sharedUserId"
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r21:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=728;drc=02a58171a9d41ad0048d6a1a48d79dee585c22a5
        hookAllMethods(signingDetails, "hasCommonAncestor", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)
                    && prefs.getBoolean("prefs_key_system_framework_core_patch_shared_user", false)
                    // because of LSPosed's bug, we can't hook verifySignatures while deoptimize it
                    && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch((o) -> "verifySignatures".equals(o.getMethodName()))
                )
                    param.setResult(true);
            }
        });

        var utilClass = findClass("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader);
        if (utilClass != null) {
            try {
                deoptimizeMethod(utilClass, "verifySignatures");
            } catch (Throwable e) {
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": deoptimize failed" + Log.getStackTraceString(e));
            }
        }

        // choose a signature after all old signed packages are removed
        var sharedUserSettingClass = XposedHelpers.findClass("com.android.server.pm.SharedUserSetting", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(sharedUserSettingClass, "removePackage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) || !prefs.getBoolean("prefs_key_system_framework_core_patch_shared_user", false))
                        return;
                    var flags = (int) XposedHelpers.getObjectField(param.thisObject, "uidFlags");
                    if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        return; // do not modify system's signature
                    var toRemove = param.args[0]; // PackageSetting
                    if (toRemove == null) return;
                    var removed = false; // Is toRemove really needed to be removed
                    var sharedUserSig = Setting_getSigningDetails(param.thisObject);
                    Object newSig = null;
                    var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.thisObject);
                    var size = (int) XposedHelpers.callMethod(packages, "size");
                    for (var i = 0; i < size; i++) {
                        var p = XposedHelpers.callMethod(packages, "valueAt", i);
                        // skip the removed package
                        if (toRemove.equals(p)) {
                            removed = true;
                            continue;
                        }
                        var packageSig = Setting_getSigningDetails(p);
                        // if old signing exists, return
                        if ((boolean) callOriginMethod(packageSig, "checkCapability", sharedUserSig, 0) || (boolean) callOriginMethod(sharedUserSig, "checkCapability", packageSig, 0)) {
                            return;
                        }
                        // otherwise, choose the first signature we meet, and merge with others if possible
                        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                        // respect to system
                        if (newSig == null) newSig = packageSig;
                        else newSig = SigningDetails_mergeLineageWith(newSig, packageSig);
                    }
                    if (!removed || newSig == null) return;
                    XposedBridge.log("updating signature in sharedUser during remove: " + param.thisObject);
                    Setting_setSigningDetails(param.thisObject, newSig);
                }
            }
        );

        XposedBridge.hookAllMethods(sharedUserSettingClass, "addPackage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true) || !prefs.getBoolean("prefs_key_system_framework_core_patch_shared_user", false))
                        return;
                    var flags = (int) XposedHelpers.getObjectField(param.thisObject, "uidFlags");
                    if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        return; // do not modify system's signature
                    var toAdd = param.args[0]; // PackageSetting
                    if (toAdd == null) return;
                    var added = false;
                    var sharedUserSig = Setting_getSigningDetails(param.thisObject);
                    Object newSig = null;
                    var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.thisObject);
                    var size = (int) XposedHelpers.callMethod(packages, "size");
                    for (var i = 0; i < size; i++) {
                        var p = XposedHelpers.callMethod(packages, "valueAt", i);
                        if (toAdd.equals(p)) {
                            // must be an existing package
                            added = true;
                            p = toAdd;
                        }
                        var packageSig = Setting_getSigningDetails(p);
                        // if old signing exists, return
                        if ((boolean) callOriginMethod(packageSig, "checkCapability", sharedUserSig, 0) || (boolean) callOriginMethod(sharedUserSig, "checkCapability", packageSig, 0)) {
                            return;
                        }
                        // otherwise, choose the first signature we meet, and merge with others if possible
                        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=193;drc=c9a8baf585e8eb0f3272443930301a61331b65c1
                        // respect to system
                        if (newSig == null) newSig = packageSig;
                        else newSig = SigningDetails_mergeLineageWith(newSig, packageSig);
                    }
                    if (!added || newSig == null) return;
                    XposedBridge.log("[HyperCeiler][D][android]" + TAG + ": updating signature in sharedUser during add " + toAdd + ": " + param.thisObject);
                    Setting_setSigningDetails(param.thisObject, newSig);
                }
            }
        );

        hookAllMethods(getIsVerificationEnabledClass(loadPackageParam.classLoader), "isVerificationEnabled", new ReturnConstant(prefs, "prefs_key_system_framework_disable_verification_agent", false));

        if (DEBUG) initializeDebugHook(loadPackageParam);
    }

    static Object callOriginMethod(Object obj, String methodName, Object... args) {
        try {
            var method = XposedHelpers.findMethodBestMatch(obj.getClass(), methodName, args);
            return XposedBridge.invokeOriginalMethod(method, obj, args);
        } catch (IllegalAccessException e) {
            // should not happen
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    Class<?> getIsVerificationEnabledClass(ClassLoader classLoader) {
        return XposedHelpers.findClass("com.android.server.pm.PackageManagerService", classLoader);
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

    Object mPMS = null;

    void initializeDebugHook(XC_LoadPackage.LoadPackageParam lpparam) throws IllegalAccessException, InvocationTargetException {
        XposedBridge.hookAllMethods(
            XposedHelpers.findClass("com.android.server.pm.PackageManagerShellCommand", lpparam.classLoader),
            "onCommand", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        var pms = mPMS;
                        if (pms == null) return;
                        var cmd = (String) param.args[0];
                        if (!"corepatch".equals(cmd)) return;
                        var self = param.thisObject;
                        var pw = (PrintWriter) XposedHelpers.callMethod(self, "getOutPrintWriter");
                        var type = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                        var settings = XposedHelpers.getObjectField(pms, "mSettings");
                        if ("p".equals(type) || "package".equals(type)) {
                            var packageName = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                            var packageSetting = XposedHelpers.callMethod(settings, "getPackageLPr", packageName);
                            if (packageSetting != null) {
                                dumpPackageSetting(packageSetting, pw, settings);
                            } else {
                                pw.println("no package " + packageName + " found");
                            }
                        } else if ("su".equals(type) || "shareduser".equals(type)) {
                            var name = (String) XposedHelpers.callMethod(self, "getNextArgRequired");
                            var su = getSharedUser(name, settings);
                            if (su != null) {
                                dumpSharedUserSetting(su, pw);
                            } else {
                                pw.println("no shared user " + name + " found");
                            }
                        } else {
                            pw.println("usage: <p|package|su|shareduser> <name>");
                        }
                        param.setResult(0);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                        param.setThrowable(t);
                    }
                }
            }
        );

        var pmsClass = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService",
            lpparam.classLoader);

        XposedBridge.hookAllConstructors(pmsClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPMS = param.thisObject;
                }
            }
        );

        deoptimizeMethod(pmsClass, "onShellCommand");
    }

    void dumpPackageSetting(Object packageSetting, java.io.PrintWriter pw, Object /*Settings*/ settings) {
        var signingDetails = Setting_getSigningDetails(packageSetting);
        pw.println("signing for package " + packageSetting);
        dumpSigningDetails(signingDetails, pw);
        var pkg = XposedHelpers.getObjectField(packageSetting, "pkg"); // AndroidPackage
        if (pkg == null) {
            pw.println("android package is null!");
            return;
        }
        var id = (String) XposedHelpers.callMethod(pkg, "getSharedUserId");
        pw.println("shared user id:" + id);
        if (settings != null) {
            var su = getSharedUser(id, settings);
            if (su != null) {
                dumpSharedUserSetting(su, pw);
            }
        }
    }

    Object getSharedUser(String id, Object /*Settings*/ settings) {
        // TODO: use Setting.getSharedUserSettingLPr(appId)?
        var sharedUserSettings = XposedHelpers.getObjectField(settings, "mSharedUsers");
        if (sharedUserSettings == null) return null;
        return XposedHelpers.callMethod(sharedUserSettings, "get", id);
    }

    void dumpSharedUserSetting(Object sharedUser, PrintWriter pw) {
        var signingDetails = Setting_getSigningDetails(sharedUser);
        pw.println("signing for shared user " + sharedUser);
        dumpSigningDetails(signingDetails, pw);
    }

    protected void dumpSigningDetails(Object signingDetails, PrintWriter pw) {
        var i = 0;
        for (var sign : (Signature[]) XposedHelpers.getObjectField(signingDetails, "signatures")) {
            i++;
            pw.println(i + ": " + sign.toCharsString());
        }
    }

    /**
     * Get signing details for PackageSetting or SharedUserSetting
     */
    Object Setting_getSigningDetails(Object pkgOrSharedUser) {
        // PackageSettingBase(A11)|PackageSetting(A13)|SharedUserSetting.<PackageSignatures>signatures.<PackageParser.SigningDetails>mSigningDetails
        return XposedHelpers.getObjectField(XposedHelpers.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails");
    }

    /**
     * Set signing details for PackageSetting or SharedUserSetting
     */
    void Setting_setSigningDetails(Object pkgOrSharedUser, Object signingDetails) {
        XposedHelpers.setObjectField(XposedHelpers.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails", signingDetails);
    }

    protected Object SharedUserSetting_packages(Object /*SharedUserSetting*/ sharedUser) {
        return XposedHelpers.getObjectField(sharedUser, "packages");
    }

    protected Object SigningDetails_mergeLineageWith(Object self, Object other) {
        return XposedHelpers.callMethod(self, "mergeLineageWith", other);
    }
}
