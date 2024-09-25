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

import android.content.pm.Signature;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForT extends CorePatchForS {
    public static final String TAG = "[CorePatchForT]";
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        var checkDowngrade = XposedHelpers.findMethodExactIfExists("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader,
            "checkDowngrade",
            "com.android.server.pm.parsing.pkg.AndroidPackage",
            "android.content.pm.PackageInfoLite");
        if (checkDowngrade != null) {
            XposedBridge.hookMethod(checkDowngrade, new ReturnConstant(prefs, "prefs_key_system_framework_core_patch_downgr", null));
        }

        Class<?> signingDetails = getSigningDetails(loadPackageParam.classLoader);
        // New package has a different signature
        // 处理覆盖安装但签名不一致
        hookAllMethods(signingDetails, "checkCapability", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Don't handle PERMISSION & AUTH
                // Or applications will have all privileged permissions
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947?q=CertCapabilities
                // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/accounts/AccountManagerService.java;l=5867
                if (prefs.getBoolean("prefs_key_system_framework_core_patch_digest_creak", true)) {
                    if ((Integer) param.args[1] != 4 && (Integer) param.args[1] != 16) {
                        param.setResult(true);
                    }
                }
            }
        });

        Class<?> ParsedPackage = getParsedPackage(loadPackageParam.classLoader);
        findAndHookMethod("com.android.server.pm.InstallPackageHelper", loadPackageParam.classLoader,
            "doesSignatureMatchForPermissions", String.class,
            ParsedPackage, int.class, new XC_MethodHook() {
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

        var assertMinSignatureSchemeIsValid = XposedHelpers.findMethodExactIfExists("com.android.server.pm.ScanPackageUtils", loadPackageParam.classLoader,
            "assertMinSignatureSchemeIsValid",
            "com.android.server.pm.parsing.pkg.AndroidPackage", int.class);
        if (assertMinSignatureSchemeIsValid != null) {
            XposedBridge.hookMethod(assertMinSignatureSchemeIsValid, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                        param.setResult(null);
                    }
                }
            });
        }

        Class<?> strictJarVerifier = findClass("android.util.jar.StrictJarVerifier", loadPackageParam.classLoader);
        if (strictJarVerifier != null) {
            XposedBridge.hookAllConstructors(strictJarVerifier, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_auth_creak", true)) {
                        XposedHelpers.setBooleanField(param.thisObject, "signatureSchemeRollbackProtectionsEnforced", false);
                    }
                }
            });
        }

        // ensure verifySignatures success
        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=621;drc=2e50991320cbef77d3e8504a4b284adae8c2f4d2
        var utils = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerServiceUtils", loadPackageParam.classLoader);
        if (utils != null) {
            deoptimizeMethod(utils, "canJoinSharedUserId");
        }
    }

    @Override
    Class<?> getIsVerificationEnabledClass(ClassLoader classLoader) {
        return XposedHelpers.findClass("com.android.server.pm.PackageManagerService", classLoader);
    }

    Class<?> getParsedPackage(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("com.android.server.pm.parsing.pkg.ParsedPackage", classLoader);
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("android.content.pm.SigningDetails", classLoader);
    }

    @Override
    protected void dumpSigningDetails(Object signingDetails, PrintWriter pw) {
        var i = 0;
        for (var sign : (Signature[]) XposedHelpers.callMethod(signingDetails, "getSignatures")) {
            i++;
            pw.println(i + ": " + sign.toCharsString());
        }
    }

    @Override
    protected Object SharedUserSetting_packages(Object sharedUser) {
        return XposedHelpers.getObjectField(sharedUser, "mPackages");
    }

    @Override
    protected Object SigningDetails_mergeLineageWith(Object self, Object other) {
        return XposedHelpers.callMethod(self, "mergeLineageWith", other, 2 /*MERGE_RESTRICTED_CAPABILITY*/);
    }
}
