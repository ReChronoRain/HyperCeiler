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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import java.lang.reflect.Method;

public class UseAndroidPackageInstaller extends BaseHook {
    static boolean fakeCts = false;
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
            }
        }
    }
}