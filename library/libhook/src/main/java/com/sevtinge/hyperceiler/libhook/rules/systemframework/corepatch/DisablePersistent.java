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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisablePersistent extends BaseHook {
    private boolean isInstall = false;

    @Override
    public void init() {
        String packageName = "com.android.server.pm.PackageSetting";

        try {
            Class<?> mPackage = findClassIfExists(packageName);

            findAndHookMethod(mPackage, "isPersistent", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    boolean isPersistent = (boolean) param.getResult();
                    if (isPersistent && isInstall) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "Not found class: " + packageName);
        }

        findAndHookMethod("com.android.server.pm.InstallPackageHelper", isMoreAndroidVersion(36) ?
            "preparePackage" : "preparePackageLI", "com.android.server.pm.InstallRequest", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                isInstall = true;
            }

            @Override
            public void after(HookParam param) {
                isInstall = false;
            }
        });
    }
}
