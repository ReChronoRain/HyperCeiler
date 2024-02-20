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
package com.sevtinge.hyperceiler.module.hook.thememanager;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class VersionCodeModify extends BaseHook {

    Class<?> mThemeApplication;

    @Override
    public void init() {

        mThemeApplication = findClassIfExists("com.android.thememanager.ThemeApplication");

        findAndHookMethod(mThemeApplication, "onCreate", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                findAndHookMethod("android.os.SystemProperties", "get", String.class, String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if ("ro.miui.ui.version.code".equals(param.args[0])) {
                            param.setResult("14");
                        }
                    }
                });
            }
        });
    }
}
