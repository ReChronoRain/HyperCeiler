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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch;

import static de.robv.android.xposed.XposedBridge.hookMethod;

import android.os.Build;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CorePatchForS extends CorePatchForR {
    public static final String TAG = "[CorePatchForS]";
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        super.handleLoadPackage(loadPackageParam);

        var pmService = XposedHelpers.findClassIfExists("com.android.server.pm.PackageManagerService",
            loadPackageParam.classLoader);
        if (pmService != null) {
            var doesSignatureMatchForPermissions = XposedHelpers.findMethodExactIfExists(pmService, "doesSignatureMatchForPermissions",
                String.class, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ? "com.android.internal.pm.parsing.pkg.ParsedPackage" : "com.android.server.pm.parsing.pkg.ParsedPackage"), int.class);
            if (doesSignatureMatchForPermissions != null) {
                hookMethod(doesSignatureMatchForPermissions, new XC_MethodHook() {
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
            }
        }
    }
}
