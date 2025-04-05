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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class ScreenRotation extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("com.android.internal.view.RotationPolicy", "areAllRotationsAllowed", Context.class, XC_MethodReplacement.returnConstant(mPrefsMap.getBoolean("system_framework_screen_all_rotations")));

        hookAllConstructors("com.android.server.wm.DisplayRotation", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setIntField(param.thisObject, "mAllowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations") ? 1 : 0);
            }
        });
    }

    public static void initRes() {
        mResHook.setObjectReplacement("android", "bool", "config_allowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
    }
}
