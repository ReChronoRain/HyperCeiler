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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.base.BaseHook.deoptimize;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class UseAndroidPackageInstaller extends BaseHook {
    private static final String ANDROID_INSTALLER = "com.android.packageinstaller";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String MIUI_INSTALLER = "com.miui.packageinstaller";
    private static final ThreadLocal<Integer> CTS_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Override
    public void init() {
        Class<?> packageManagerServiceImpl = findClassIfExists("com.android.server.pm.PackageManagerServiceImpl");
        Class<?> packageManagerServiceStub = findClassIfExists("com.android.server.pm.PackageManagerServiceStub");
        if (packageManagerServiceImpl == null && packageManagerServiceStub == null) {
            XposedLog.w(TAG, "find class E com.android.server.pm.PackageManagerServiceImpl");
            return;
        }

        if (packageManagerServiceImpl != null) {
            hookAllMethods(packageManagerServiceImpl, "isCTS", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(CTS_DEPTH.get() > 0);
                }
            });
        }

        IMethodHook chooseBestActivity = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                enterCtsBranch();
                forceAndroidInstaller(param.getThisObject());
            }
            @Override
            public void after(HookParam param) {
                forceAndroidInstaller(param.getThisObject());
                forceAndroidResolve(param);
                leaveCtsBranch();
            }
        };
        IMethodHook repairState = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                enterCtsBranch();
            }

            @Override
            public void after(HookParam param) {
                forceAndroidInstaller(param.getThisObject());
                leaveCtsBranch();
            }
        };

        hookMethods(packageManagerServiceImpl, chooseBestActivity, repairState);
        hookMethods(packageManagerServiceStub, chooseBestActivity, repairState);

        // The methods are frequently compiled into boot/system_server oat.
        // Deoptimizing the concrete implementation guarantees that the hooks
        // are reached after a system_server restart.
        deoptimizeNamedMethods(packageManagerServiceImpl);
        deoptimizeNamedMethods(packageManagerServiceStub);
    }

    private void enterCtsBranch() {
        CTS_DEPTH.set(CTS_DEPTH.get() + 1);
    }

    private void leaveCtsBranch() {
        int depth = CTS_DEPTH.get() - 1;
        if (depth <= 0) CTS_DEPTH.remove();
        else CTS_DEPTH.set(depth);
    }

    private void hookMethods(Class<?> clazz, IMethodHook chooseBestActivity, IMethodHook repairState) {
        if (clazz == null) return;
        hookAllMethods(clazz, "hookChooseBestActivity", chooseBestActivity);
        hookAllMethods(clazz, "updateDefaultPkgInstallerLocked", repairState);
        hookAllMethods(clazz, "switchPackageInstaller", repairState);
        hookAllMethods(clazz, "assertValidApkAndInstaller", repairState);
    }

    private void deoptimizeNamedMethods(Class<?> clazz) {
        if (clazz == null) return;
        for (Method method : clazz.getDeclaredMethods()) {
            String name = method.getName();
            if ("isCTS".equals(name)
                    || "hookChooseBestActivity".equals(name)
                    || "updateDefaultPkgInstallerLocked".equals(name)
                    || "switchPackageInstaller".equals(name)
                    || "assertValidApkAndInstaller".equals(name)) {
                try {
                    deoptimize(method);
                } catch (Throwable t) {
                    XposedLog.e(TAG, getPackageName(), t);
                }
            }
        }
    }

    private void forceAndroidResolve(HookParam param) {
        Object[] args = param.getArgs();
        if (args.length < 4 || !(args[0] instanceof Intent intent)
                || !APK_MIME_TYPE.equals(intent.getType())
                || !(args[3] instanceof List<?> query)) {
            return;
        }

        // hookChooseBestActivity receives the complete candidate list. Pick
        // the explicit AOSP candidate after MIUI has made its own choice.
        // This avoids a second resolver call and therefore cannot recurse.
        for (Object item : query) {
            if (item instanceof ResolveInfo resolveInfo
                    && resolveInfo.activityInfo != null
                    && ANDROID_INSTALLER.equals(resolveInfo.activityInfo.packageName)) {
                param.setResult(resolveInfo);
                intent.setPackage(ANDROID_INSTALLER);
                return;
            }
        }
    }

    /**
     * MIUI normally disables the AOSP installer for user 0 and leaves
     * mCurrentPackageInstaller pointing at the MIUI package.  The CTS branch
     * only helps when updateDefaultPkgInstallerLocked() has already repaired
     * that state, so repair the same small piece of state after the method and
     * before an APK intent is resolved.
     */
    private void forceAndroidInstaller(Object service) {
        try {
            Object pkgSettings = getObjectField(service, "mPkgSettings");
            Object packages = getObjectField(pkgSettings, "mPackages");
            Object setting = callMethod(packages, "get", ANDROID_INSTALLER);
            if (setting == null) return;

            // PackageSetting is present in the system package map even when
            // MIUI marked it uninstalled for the current user.
            callMethod(setting, "setInstalled", true, 0);
            Object pms = getObjectField(service, "mPms");
            Object pmsPackages = getObjectField(pms, "mPackages");
            Object androidPackage = callMethod(pmsPackages, "get", ANDROID_INSTALLER);
            if (androidPackage != null) {
                callMethod(pmsPackages, "put", ANDROID_INSTALLER, androidPackage);
            }
            setObjectField(service, "mCurrentPackageInstaller", ANDROID_INSTALLER);
            setObjectField(pms, "mRequiredInstallerPackage", ANDROID_INSTALLER);
            setObjectField(pms, "mRequiredUninstallerPackage", ANDROID_INSTALLER);

            // Keep the old MIUI setting disabled for user 0 when it is still
            // present. The resolver can then no longer select its front end.
            Object miuiSetting = callMethod(packages, "get", MIUI_INSTALLER);
            if (miuiSetting != null) {
                callMethod(miuiSetting, "setInstalled", false, 0);
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, "Unable to select the AOSP package installer", t);
        }
    }
}
