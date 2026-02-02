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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimize;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class UseAndroidPackageInstaller extends BaseHook {
    static boolean fakeCts = false;

    @Override
    public void init() {
        Class<?> PackageManagerServiceImpl = findClassIfExists("com.android.server.pm.PackageManagerServiceImpl");
        if (PackageManagerServiceImpl == null) {
            XposedLog.w(TAG, "find class E com.android.server.pm.PackageManagerServiceImpl");
            return;
        }

        findAndHookMethod(PackageManagerServiceImpl, "isCTS", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                // logD(TAG, packageName,"fakeCTS:"+ fakeCts);
                param.setResult(fakeCts);
            }
        });

        Method[] methods = PackageManagerServiceImpl.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            if ("hookChooseBestActivity".equals(name) || "updateDefaultPkgInstallerLocked".equals(name) || "assertValidApkAndInstaller".equals(name)) {
                // logD(TAG, packageName,"hook " + name);
                hookMethod(method, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        fakeCts = true;
                    }

                    @Override
                    public void after(AfterHookParam param) {
                        fakeCts = false;
                    }
                });

                try {
                    deoptimize(method);
                } catch (Throwable t) {
                    XposedLog.e("UseAndroidPackageInstaller", getPackageName(), t);
                }
            }
        }
    }
}
