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
    Integer mChargeVersionCode;

    @Override
    public void init() {
        mThemeApplication = findClassIfExists("com.android.thememanager.ThemeApplication");
        mChargeVersionCode = mPrefsMap.getStringAsInt("theme_manager_new_version_code_modify", 0);

        findAndHookMethod(mThemeApplication, "onCreate", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {

                findAndHookMethod("android.os.SystemProperties", "get", String.class, String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        // 待定，等找个时间完善
                        if ("ro.miui.ui.version.code".equals(param.args[0])) {
                            switch (mChargeVersionCode) {
                                case 110 -> param.setResult("110");
                                case 120 -> param.setResult("120");
                                case 125 -> param.setResult("125");
                                case 130 -> param.setResult("130");
                                case 140 -> param.setResult("140");
                                case 150 -> param.setResult("816");
                            }
                        }
                    }
                });
            }
        });
    }
}
