package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.deoptimizeMethods;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.findField;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.setIntField;

import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.libxposed.api.XposedModuleInterface;

public class SharedUserPatch extends CorePatchHelper {

    private static final String TAG = "SharedUserPatch";

    public void init(XposedModuleInterface.SystemServerStartingParam lpparam) {
        // Android 14+
        try {
            var utilClass = findClass("com.android.server.pm.ReconcilePackageUtils", lpparam.getClassLoader());
            if (utilClass != null) {
                deoptimizeMethods(utilClass, "reconcilePackages");
            }

            // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r60:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java;l=61;bpv=1;bpt=0
            if (CorePatchHelper.isSharedUserEnabled()) {
                try {
                    var field = findField(utilClass, "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS");
                    int accessFlags = (int) getIntField(field, "accessFlags");

                    setIntField(field, "accessFlags", accessFlags & ~Modifier.FINAL);
                    field.set(null, true);
                } catch (Throwable e) {
                    XposedLog.e(TAG, "system", "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS failed" + Log.getStackTraceString(e));
                }
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 14+ hook failed, crash: " + t);
        }

        // Android 11+
        try {
            Class<?> signingDetails = getSigningDetails(lpparam.getClassLoader());
            // for SharedUser
            // "Package " + packageName + " has a signing lineage " + "that diverges from the lineage of the sharedUserId"
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r21:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=728;drc=02a58171a9d41ad0048d6a1a48d79dee585c22a5
            hookAllMethods(signingDetails, "hasCommonAncestor", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (CorePatchHelper.isSharedUserEnabled()
                        // because of LSPosed's bug, we can't hook verifySignatures while deoptimize it
                        && Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch((o) -> "verifySignatures".equals(o.getMethodName()))
                    )
                        param.setResult(true);
                }
            });

            var utilClass = findClass("com.android.server.pm.PackageManagerServiceUtils", lpparam.getClassLoader());
            if (utilClass != null) {
                deoptimizeMethods(utilClass, "verifySignatures");
                hookVerifySignatures(utilClass);
            }

            // choose a signature after all old signed packages are removed
            var sharedUserSettingClass = findClass("com.android.server.pm.SharedUserSetting", lpparam.getClassLoader());
            hookAllMethods(sharedUserSettingClass, "removePackage", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        if (!CorePatchHelper.isSharedUserEnabled())
                            return;
                        var flags = (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "uidFlags");
                        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            return; // do not modify system's signature
                        var toRemove = param.getArgs()[0]; // PackageSetting
                        if (toRemove == null) return;
                        var removed = false; // Is toRemove really needed to be removed
                        var sharedUserSig = Setting_getSigningDetails(param.getThisObject());
                        Object newSig = null;
                        var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.getThisObject());
                        var size = (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(packages, "size");
                        for (var i = 0; i < size; i++) {
                            var p = com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(packages, "valueAt", i);
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
                        XposedLog.w(TAG, "system", "updating signature in sharedUser during remove: " + param.getThisObject());
                        Setting_setSigningDetails(param.getThisObject(), newSig);
                    }
                }
            );

            hookAllMethods(sharedUserSettingClass, "addPackage", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        if (!CorePatchHelper.isSharedUserEnabled())
                            return;
                        var flags = (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "uidFlags");
                        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                            return; // do not modify system's signature
                        var toAdd = param.getArgs()[0]; // PackageSetting
                        if (toAdd == null) return;
                        var added = false;
                        var sharedUserSig = Setting_getSigningDetails(param.getThisObject());
                        Object newSig = null;
                        var packages = /*Watchable?ArraySet<PackageSetting>*/ SharedUserSetting_packages(param.getThisObject());
                        var size = (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(packages, "size");
                        for (var i = 0; i < size; i++) {
                            var p = com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(packages, "valueAt", i);
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
                        XposedLog.w(TAG, "system", "updating signature in sharedUser during add " + toAdd + ": " + param.getThisObject());
                        Setting_setSigningDetails(param.getThisObject(), newSig);
                    }
                }
            );
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

    static Object callOriginMethod(Object obj, String methodName, Object... args) {
        try {
            var method = com.sevtinge.hyperceiler.libhook.base.BaseHook.findMethodBestMatch(obj.getClass(), methodName, args);
            return com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeOriginalMethod(method, obj, args);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * Get signing details for PackageSetting or SharedUserSetting
     */
    Object Setting_getSigningDetails(Object pkgOrSharedUser) {
        // PackageSettingBase(A11)|PackageSetting(A13)|SharedUserSetting.<PackageSignatures>signatures.<PackageParser.SigningDetails>mSigningDetails
        return com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails");
    }

    /**
     * Set signing details for PackageSetting or SharedUserSetting
     */
    void Setting_setSigningDetails(Object pkgOrSharedUser, Object signingDetails) {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.setObjectField(com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(pkgOrSharedUser, "signatures"), "mSigningDetails", signingDetails);
    }

    protected Object SharedUserSetting_packages(Object /*SharedUserSetting*/ sharedUser) {
        if (isMoreAndroidVersion(33)) {
            return com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(sharedUser, "mPackages");
        }
        return com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(sharedUser, "packages");
    }

    protected Object SigningDetails_mergeLineageWith(Object self, Object other) {
        if (isMoreAndroidVersion(33)) {
            return com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(self, "mergeLineageWith", other, 2 /*MERGE_RESTRICTED_CAPABILITY*/);
        }
        return com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(self, "mergeLineageWith", other);
    }

    private void hookVerifySignatures(Class<?> utilClass) {
        for (Method method : utilClass.getDeclaredMethods()) {
            if (!"verifySignatures".equals(method.getName()) || method.getReturnType() != Boolean.TYPE) {
                continue;
            }
            com.sevtinge.hyperceiler.libhook.base.BaseHook.hookMethod(method, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (CorePatchHelper.isFeatureEnabled(CorePatchHelper.PREF_AUTH_CREAK, false)) {
                        param.setResult(false);
                    }
                }
            });
        }
    }
}
