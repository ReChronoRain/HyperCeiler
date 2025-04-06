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

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class UseAndroidPackageInstaller extends BaseHook {
    static boolean fakeCts = false;

    private final static Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            logE("UseAndroidPackageInstaller", "android", t);
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Method m) throws InvocationTargetException, IllegalAccessException {
        if (deoptimizeMethod != null) {
            deoptimizeMethod.invoke(null, m);
            logD("UseAndroidPackageInstaller", "android", "Deoptimized " + m);
        }
    }

    @Override
    public void init() {
        Class<?> PackageManagerServiceImpl = findClassIfExists("com.android.server.pm.PackageManagerServiceImpl");
        if (PackageManagerServiceImpl == null) {
            logE(TAG, "find class E com.android.server.pm.PackageManagerServiceImpl");
            return;
        }

        findAndHookMethod(PackageManagerServiceImpl, "isCTS", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                // logD(TAG, lpparam.packageName,"fakeCTS:"+ fakeCts);
                param.setResult(fakeCts);
            }
        });

        Method[] methods = PackageManagerServiceImpl.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            if ("hookChooseBestActivity".equals(name) || "updateDefaultPkgInstallerLocked".equals(name) || "assertValidApkAndInstaller".equals(name)) {
                // logD(TAG, lpparam.packageName,"hook " + name);
                hookMethod(method, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        fakeCts = true;
                    }

                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        fakeCts = false;
                    }
                });

                if (isMoreHyperOSVersion(2f)) {
                    try {
                        deoptimizeMethod(method);
                    } catch (Throwable t) {
                        logE("UseAndroidPackageInstaller", "android", t);
                    }
                }
            }
        }
    }
}
