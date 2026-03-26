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
package com.sevtinge.hyperceiler.hook.module.rules.packageinstaller;

import android.app.Activity;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class InstallSource extends BaseHook {

    String mInstallSourcePackageName;

    @Override
    public void init() {

        mInstallSourcePackageName = mPrefsMap.getString("miui_package_installer_install_source", "com.android.fileexplorer");

        findAndHookMethod(Activity.class, "getLaunchedFromPackage", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (isCalledFromInstallStart()) param.setResult(mInstallSourcePackageName);
            }
        });

        findAndHookMethodSilently("com.miui.packageInstaller.InstallStart",
            "getCallingPackage",
            XC_MethodReplacement.returnConstant(mInstallSourcePackageName));
    }

    private static boolean isCalledFromInstallStart() {
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            if (st == null) return false;

            for (int i = 0; i < st.length - 1; i++) {
                StackTraceElement cur = st[i];
                StackTraceElement next = st[i + 1];

                if (cur == null || next == null) continue;

                if (cur.getClassName() != null
                    && cur.getClassName().startsWith("com.miui.packageInstaller")) {

                    if ("com.miui.packageInstaller.InstallStart".equals(next.getClassName())
                        && "onCreate".equals(next.getMethodName())) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            de.robv.android.xposed.XposedBridge.log("isCalledFromInstallStartStrict error: " + t);
        }
        return false;
    }

}
