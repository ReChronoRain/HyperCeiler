package com.sevtinge.cemiuiler.module.systemframework.corepatch;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import com.sevtinge.cemiuiler.BuildConfig;

import android.util.Log;

import com.sevtinge.cemiuiler.module.SystemFrameworkForCorepatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;
import static com.sevtinge.cemiuiler.utils.Helpers.log;

public class CorePatchForR extends XposedHelper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, "conf");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        log("CorePatchForR Downgrade=" + mPrefsMap.getBoolean("system_framework_core_patch_downgr"));
        log("CorePatchForR AuthCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_auth_creak"));
        log("CorePatchForR DigestCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_digest_creak"));
        log("CorePatchForR UsePreSig=" + mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature"));
        log("CorePatchForR EnhancedMode=" + mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode"));

        // 允许降级
        findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,//!
                "checkDowngrade",
                "com.android.server.pm.parsing.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", null));

        // exists on flyme 9(Android 11) only
        findAndHookMethod("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader,//!
                "checkDowngrade",
                "android.content.pm.PackageInfoLite",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", true));
        
        hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader, "isDowngradePermitted",
                new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", true));


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
        findAndHookMethod("com.android.apksig.ApkVerifier", loadPackageParam.classLoader, "getMinimumSignatureSchemeVersionForTargetSdk", int.class,
                new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_auth_creak", 0));//!

        // Package " + packageName + " signatures do not match previously installed version; ignoring!"
        // public boolean checkCapability(String sha256String, @CertCapabilities int flags) {
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)
        hookAllMethods("android.content.pm.PackageParser", loadPackageParam.classLoader, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
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
        Class<?> parseResult = XposedHelpers.findClassIfExists("android.content.pm.parsing.result.ParseResult", loadPackageParam.classLoader);
        hookAllMethods("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader, "verifyBytes", new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    if (!mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
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
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
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
                            if (mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
                                PackageManager PM = AndroidAppHelper.currentApplication().getPackageManager();
                                if (PM == null) {
                                    log("CorePatchForR E: " + BuildConfig.APPLICATION_ID + " Cannot get the Package Manager... Are you using MiUI?");
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
                            if (lastSigs == null && mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
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

                        //修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                        Class<?> signingDetailsWithDigests = XposedHelpers.findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests", loadPackageParam.classLoader);
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> signingDetailsWithDigestsConstructorExact = XposedHelpers.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
                            signingDetailsWithDigestsConstructorExact.setAccessible(true);
                            newInstance = signingDetailsWithDigestsConstructorExact.newInstance(new Object[]{newInstance, null});
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
        // if app is system app, allow to use hidden api, even if app not using a system signature
        findAndHookMethod("android.content.pm.ApplicationInfo", loadPackageParam.classLoader, "isPackageWhitelistedForHiddenApis", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
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
            for (var m : utilClass.getDeclaredMethods()) {
                if ("verifySignatures".equals(m.getName())) {
                    try {
                        XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class).invoke(null, m);
                    } catch (Throwable e) {
                        Log.e("CorePatch", "deoptimizing failed", e);
                    }
                }
            }
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
                if (mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode")) {
                    super.beforeHookedMethod(param);
                    param.args[3] = Boolean.FALSE;
                }
            }
        });
    }
}

/*public class CorePatchForR extends BaseHook {

    public String SIGNATURE = "308203c6308202aea003020102021426d148b7c65944abcf3a683b4c3dd3b139c4ec85300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3139303130323138353233385a170d3439303130323138353233385a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820122300d06092a864886f70d01010105000382010f003082010a028201010087fcde48d9beaeba37b733a397ae586fb42b6c3f4ce758dc3ef1327754a049b58f738664ece587994f1c6362f98c9be5fe82c72177260c390781f74a10a8a6f05a6b5ca0c7c5826e15526d8d7f0e74f2170064896b0cf32634a388e1a975ed6bab10744d9b371cba85069834bf098f1de0205cdee8e715759d302a64d248067a15b9beea11b61305e367ac71b1a898bf2eec7342109c9c5813a579d8a1b3e6a3fe290ea82e27fdba748a663f73cca5807cff1e4ad6f3ccca7c02945926a47279d1159599d4ecf01c9d0b62e385c6320a7a1e4ddc9833f237e814b34024b9ad108a5b00786ea15593a50ca7987cbbdc203c096eed5ff4bf8a63d27d33ecc963990203010001a350304e300c0603551d13040530030101ff301d0603551d0e04160414a361efb002034d596c3a60ad7b0332012a16aee3301f0603551d23041830168014a361efb002034d596c3a60ad7b0332012a16aee3300d06092a864886f70d01010b0500038201010022ccb684a7a8706f3ee7c81d6750fd662bf39f84805862040b625ddf378eeefae5a4f1f283deea61a3c7f8e7963fd745415153a531912b82b596e7409287ba26fb80cedba18f22ae3d987466e1fdd88e440402b2ea2819db5392cadee501350e81b8791675ea1a2ed7ef7696dff273f13fb742bb9625fa12ce9c2cb0b7b3d94b21792f1252b1d9e4f7012cb341b62ff556e6864b40927e942065d8f0f51273fcda979b8832dd5562c79acf719de6be5aee2a85f89265b071bf38339e2d31041bc501d5e0c034ab1cd9c64353b10ee70b49274093d13f733eb9d3543140814c72f8e003f301c7a00b1872cc008ad55e26df2e8f07441002c4bcb7dc746745f0db";

    Class<?> mApkVerifier;

    @Override
    public void init() {

        mApkVerifier = findClassIfExists("com.android.apksig.ApkVerifier");

        // 允许降级
        if(!isAndroidVersionTiramisu()) {
            findAndHookMethod("com.android.server.pm.PackageManagerService",
                    "checkDowngrade",
                    "com.android.server.pm.parsing.pkg.AndroidPackage",
                    "android.content.pm.PackageInfoLite",
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (mPrefsMap.getBoolean("system_framework_core_patch_downgr")) {
                                param.setResult(null);
                            }
                        }
                    });
        }


        // apk内文件修改后 digest校验会失败
        hookAllMethods("android.util.jar.StrictJarVerifier", "verifyMessageDigest", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(true);
                }
            }
        });

        hookAllMethods("android.util.jar.StrictJarVerifier", "verify", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(true);
                }
            }
        });

        hookAllMethods("java.security.MessageDigest", "isEqual", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(true);
                }
            }
        });

        hookAllMethods("android.content.res.AssetManager", "containsAllocatedTable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(false);
                }
            }
        });

        findAndHookMethod("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk", int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    param.setResult(0);
                }
            }
        });

        if (mApkVerifier != null) {
            findAndHookMethod(mApkVerifier, "getMinimumSignatureSchemeVersionForTargetSdk", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                        param.setResult(0);
                    }
                }
            });
        }

        hookAllMethods("android.content.pm.PackageParser", "checkCapability", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {

                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    if ((Integer) param.args[1] != 4) {
                        param.setResult(true);
                    }
                }
            }
        });



        // 当verifyV1Signature抛出转换异常时，替换一个签名作为返回值
        // 如果用户已安装apk，并且其定义了私有权限，则安装时会因签名与模块内硬编码的不一致而被拒绝。尝试从待安装apk中获取签名。如果其中apk的签名和已安装的一致（只动了内容）就没有问题。此策略可能有潜在的安全隐患。
        Class<?> pkc = findClass("sun.security.pkcs.PKCS7");
        Constructor<?> constructor = XposedHelpers.findConstructorExact(pkc, byte[].class);
        constructor.setAccessible(true);
        Class<?> ASV = findClass("android.util.apk.ApkSignatureVerifier");
        Class<?> sJarClass = findClass("android.util.jar.StrictJarFile");
        Constructor<?> constructorExact = XposedHelpers.findConstructorExact(sJarClass, String.class, boolean.class, boolean.class);
        constructorExact.setAccessible(true);
        Class<?> signingDetails = findClass("android.content.pm.PackageParser.SigningDetails");
        Constructor<?> findConstructorExact = XposedHelpers.findConstructorExact(signingDetails, Signature[].class, Integer.TYPE);
        findConstructorExact.setAccessible(true);
        Class<?> packageParserException = findClass("android.content.pm.PackageParser.PackageParserException");
        Field error = XposedHelpers.findField(packageParserException, "error");
        error.setAccessible(true);
        Object[] signingDetailsArgs = new Object[2];
        signingDetailsArgs[1] = 1;

        hookAllMethods("android.util.jar.StrictJarVerifier", "verifyBytes", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    if (!mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
                        final Object block = constructor.newInstance(param.args[0]);
                        Object[] infos = (Object[]) XposedHelpers.callMethod(block, "getSignerInfos");
                        Object info = infos[0];
                        List<X509Certificate> verifiedSignerCertChain = (List<X509Certificate>) XposedHelpers.callMethod(info, "getCertificateChain", block);
                        param.setResult(verifiedSignerCertChain.toArray(new X509Certificate[0]));
                    }
                }
            }
        });

        hookAllMethods("android.util.apk.ApkSignatureVerifier", "verifyV1Signature", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak")) {
                    Throwable throwable = param.getThrowable();
                    if (throwable != null) {
                        Signature[] lastSigs = null;
                        if (mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
                            PackageManager PM = AndroidAppHelper.currentApplication().getPackageManager();
                            if (PM == null) {
                                LogUtils.log("E: Cannot get the Package Manager... Are you using MiUI?");
                            } else {
                                PackageInfo pI = PM.getPackageArchiveInfo((String) param.args[0], 0);
                                PackageInfo InstpI = PM.getPackageInfo(pI.packageName, PackageManager.GET_SIGNATURES);
                                lastSigs = InstpI.signatures;
                            }
                        } else {
                            if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                                final Object origJarFile = constructorExact.newInstance(param.args[0], true, false);
                                final ZipEntry manifestEntry = (ZipEntry) XposedHelpers.callMethod(origJarFile, "findEntry", "AndroidManifest.xml");
                                final Certificate[][] lastCerts = (Certificate[][]) XposedHelpers.callStaticMethod(ASV, "loadCertificates", origJarFile, manifestEntry);
                                lastSigs = (Signature[]) XposedHelpers.callStaticMethod(ASV, "convertToSignatures", (Object) lastCerts);
                            }
                        }
                        if (lastSigs != null) {
                            signingDetailsArgs[0] = lastSigs;
                        } else {
                            signingDetailsArgs[0] = new Signature[]{new Signature(SIGNATURE)};
                        }
                        Object newInstance = findConstructorExact.newInstance(signingDetailsArgs);

                        //修复 java.lang.ClassCastException: Cannot cast android.content.pm.PackageParser$SigningDetails to android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests
                        Class<?> signingDetailsWithDigests = findClassIfExists("android.util.apk.ApkSignatureVerifier.SigningDetailsWithDigests");
                        if (signingDetailsWithDigests != null) {
                            Constructor<?> signingDetailsWithDigestsConstructorExact = XposedHelpers.findConstructorExact(signingDetailsWithDigests, signingDetails, Map.class);
                            signingDetailsWithDigestsConstructorExact.setAccessible(true);
                            newInstance = signingDetailsWithDigestsConstructorExact.newInstance(new Object[]{newInstance, null});
                        }

                        Throwable cause = throwable.getCause();
                        if (throwable.getClass() == packageParserException) {
                            if (error.getInt(throwable) == -103) {
                                param.setResult(newInstance);
                            }
                        }
                        if (cause != null && cause.getClass() == packageParserException) {
                            if (error.getInt(cause) == -103) {
                                param.setResult(newInstance);
                            }
                        }
                    }
                }
            }
        });


        //New package has a different signature
        //处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (((Integer) param.args[1] != 4) && mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    param.setResult(true);
                }
            }
        });

        // if app is system app, allow to use hidden api, even if app not using a system signature
        findAndHookMethod("android.content.pm.ApplicationInfo", "isPackageWhitelistedForHiddenApis", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak")) {
                    ApplicationInfo info = (ApplicationInfo) param.thisObject;
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        param.setResult(true);
                    }
                }
            }
        });
    }



    public static void initZygote() {
        Helpers.hookAllMethods("android.content.pm.PackageParser", null, "getApkSigningVersion", XC_MethodReplacement.returnConstant(1));
        Helpers.hookAllConstructors("android.util.jar.StrictJarVerifier", null, new Helpers.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode")) {
                    super.before(param);
                    param.args[3] = Boolean.FALSE;
                }
            }
        });
    }


    }*/
