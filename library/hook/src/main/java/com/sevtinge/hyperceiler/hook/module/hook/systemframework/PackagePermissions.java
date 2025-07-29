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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class PackagePermissions extends BaseHook {
    private final ArrayList<String> systemPackages = new ArrayList<>();

    @Override
    public void init() {
        systemPackages.add(ProjectApi.mAppModulePkg);

        // Allow signature level permissions for module
        String PMSCls = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.server.pm.permission.PermissionManagerServiceImpl" : "com.android.server.pm.permission.PermissionManagerService";

        // Allow signature level permissions for module
        hookAllMethods(PMSCls, "shouldGrantPermissionBySignature", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String pkgName = (String) XposedHelpers.callMethod(param.args[0], "getPackageName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        hookAllMethodsSilently("com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String pkgName = (String) XposedHelpers.callMethod(param.args[0], "getName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        // Make module appear as system app
        String ActQueryService = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.server.pm.ComputerEngine" : "com.android.server.pm.PackageManagerService";
        hookAllMethods(ActQueryService, "queryIntentActivitiesInternal", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(MethodHookParam param) {
                if (param.args.length < 6) return;
                List<ResolveInfo> infos = (List<ResolveInfo>) param.getResult();
                if (infos != null) {
                    for (ResolveInfo info : infos) {
                        if (info != null && info.activityInfo != null && systemPackages.contains(info.activityInfo.packageName)) {
                            XposedHelpers.setObjectField(info, "system", true);
                        }
                    }
                }
            }
        });

        findAndHookMethod("android.content.pm.ApplicationInfo", "isSystemApp", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                ApplicationInfo ai = (ApplicationInfo) param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        findAndHookMethodSilently("android.content.pm.ApplicationInfo", "isSignedWithPlatformKey", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                ApplicationInfo ai = (ApplicationInfo) param.thisObject;
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        try {
            Class<?> dpgpiClass = findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy");
            String[] MIUI_SYSTEM_APPS = (String[]) XposedHelpers.getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS");
            ArrayList<String> mySystemApps = new ArrayList<>(Arrays.asList(MIUI_SYSTEM_APPS));
            mySystemApps.addAll(systemPackages);
            XposedHelpers.setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
        } catch (Throwable t) {
            logW(TAG, this.lpparam.packageName, t);
        }
    }
}
