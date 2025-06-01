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

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class DisablePersistent extends BaseHook {
    private boolean isInstall = false;

    @Override
    public void init() {
        String packageName = isMoreAndroidVersion(35) ? "com.android.server.pm.PackageSetting"
            : "com.android.server.pm.parsing.pkg.PackageImpl";

        try {
            Class<?> mPackage = findClassIfExists(packageName);

            findAndHookMethod(mPackage, "isPersistent", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    boolean isPersistent = (boolean) param.getResult();
                    if (isPersistent && isInstall) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            logE(TAG, lpparam.packageName, "Not found class: " + packageName);
        }

        findAndHookMethod("com.android.server.pm.InstallPackageHelper", "preparePackageLI", "com.android.server.pm.InstallRequest", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                isInstall = true;
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                isInstall = false;
            }
        });
    }
}
