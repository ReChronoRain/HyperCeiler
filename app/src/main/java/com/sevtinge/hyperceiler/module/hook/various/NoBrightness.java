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
package com.sevtinge.hyperceiler.module.hook.various;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NoBrightness extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("android.view.Window",
            "setAttributes",
            "android.view.WindowManager$LayoutParams",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    // logE(TAG, "setAttributes: " + param.args[0]);
                    Object layoutParams = param.args[0];
                    XposedHelpers.setObjectField(layoutParams, "screenBrightness", -1.0f);
                    // param.setResult(null);
                }
            }
        );

        findAndHookMethod("android.view.WindowManager$LayoutParams",
            "copyFrom",
            "android.view.WindowManager$LayoutParams",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    // logE(TAG, "copyFrom: " + param.args[0]);
                    Object layoutParams = param.args[0];
                    XposedHelpers.setObjectField(layoutParams, "screenBrightness", -1.0f);
                }
            }
        );

    }
}
