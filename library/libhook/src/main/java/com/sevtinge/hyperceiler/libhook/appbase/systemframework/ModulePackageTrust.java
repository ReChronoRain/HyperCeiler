/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.appbase.systemframework;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

public class ModulePackageTrust extends BaseHook {
    private final ArrayList<String> systemPackages = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        systemPackages.add(ProjectApi.mAppModulePkg);

        chainAllMethods("com.android.server.pm.permission.PermissionManagerServiceImpl",
            "shouldGrantPermissionBySignature",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    String packageName = (String) callMethod(chain.getArg(0), "getPackageName");
                    if (systemPackages.contains(packageName)) {
                        return true;
                    }
                    return chain.proceed();
                }
            });

        chainAllMethods("com.android.server.pm.PackageManagerServiceUtils",
            "verifySignatures",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    String packageName = (String) callMethod(chain.getArg(0), "getName");
                    if (systemPackages.contains(packageName)) {
                        return true;
                    }
                    return chain.proceed();
                }
            });

        chainAllMethods("com.android.server.pm.ComputerEngine",
            "queryIntentActivitiesInternal",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    if (chain.getArgs().size() < 6 || !(result instanceof List<?> infos)) {
                        return result;
                    }
                    for (Object item : infos) {
                        if (!(item instanceof ResolveInfo info)) {
                            continue;
                        }
                        if (info.activityInfo == null) {
                            continue;
                        }
                        if (systemPackages.contains(info.activityInfo.packageName)) {
                            setObjectField(info, "system", true);
                        }
                    }
                    return result;
                }
            });

        findAndChainMethod("android.content.pm.ApplicationInfo", "isSystemApp", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                ApplicationInfo applicationInfo = (ApplicationInfo) chain.getThisObject();
                if (applicationInfo != null && systemPackages.contains(applicationInfo.packageName)) {
                    return true;
                }
                return result;
            }
        });

        findAndChainMethod("android.content.pm.ApplicationInfo", "isSignedWithPlatformKey", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                ApplicationInfo applicationInfo = (ApplicationInfo) chain.getThisObject();
                if (applicationInfo != null && systemPackages.contains(applicationInfo.packageName)) {
                    return true;
                }
                return result;
            }
        });

        try {
            Class<?> policyClass = findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy");
            String[] miuiSystemApps = (String[]) getStaticObjectField(policyClass, "MIUI_SYSTEM_APPS");
            ArrayList<String> mySystemApps = new ArrayList<>(Arrays.asList(miuiSystemApps));
            mySystemApps.addAll(systemPackages);
            setStaticObjectField(policyClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), t);
        }
    }
}
