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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class PackagePermissions extends BaseHook {
    private final ArrayList<String> systemPackages = new ArrayList<>();

    @Override
    public void init() {
        systemPackages.add(ProjectApi.mAppModulePkg);

        // Allow signature level permissions for module
        String PMSCls = "com.android.server.pm.permission.PermissionManagerServiceImpl";

        // Allow signature level permissions for module
        hookAllMethods(PMSCls, "shouldGrantPermissionBySignature", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                String pkgName = (String) callMethod(param.getArgs()[0], "getPackageName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        hookAllMethods("com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                String pkgName = (String) callMethod(param.getArgs()[0], "getName");
                if (systemPackages.contains(pkgName)) param.setResult(true);
            }
        });


        // Make module appear as system app
        String ActQueryService = "com.android.server.pm.ComputerEngine";
        hookAllMethods(ActQueryService, "queryIntentActivitiesInternal", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (param.getArgs().length < 6) return;
                List<ResolveInfo> infos = (List<ResolveInfo>) param.getResult();
                if (infos != null) {
                    for (ResolveInfo info : infos) {
                        if (info != null && info.activityInfo != null && systemPackages.contains(info.activityInfo.packageName)) {
                            setObjectField(info, "system", true);
                        }
                    }
                }
            }
        });

        findAndHookMethod("android.content.pm.ApplicationInfo", "isSystemApp", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ApplicationInfo ai = (ApplicationInfo) param.getThisObject();
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        findAndHookMethod("android.content.pm.ApplicationInfo", "isSignedWithPlatformKey", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ApplicationInfo ai = (ApplicationInfo) param.getThisObject();
                if (ai != null && systemPackages.contains(ai.packageName)) {
                    param.setResult(true);
                }
            }
        });

        try {
            Class<?> dpgpiClass = findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy");
            String[] MIUI_SYSTEM_APPS = (String[]) getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS");
            ArrayList<String> mySystemApps = new ArrayList<>(Arrays.asList(MIUI_SYSTEM_APPS));
            mySystemApps.addAll(systemPackages);
            setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), t);
        }
    }
}
