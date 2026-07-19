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

import android.content.Context;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class ScreenRotation extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.android.internal.view.RotationPolicy", "areAllRotationsAllowed", Context.class, returnConstant(PrefsBridge.getBoolean("system_framework_screen_all_rotations")));

        hookAllConstructors("com.android.server.wm.DisplayRotation", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                com.sevtinge.hyperceiler.libhook.base.BaseHook.setIntField(param.getThisObject(), "mAllowAllRotations", PrefsBridge.getBoolean("system_framework_screen_all_rotations") ? 1 : 0);
            }
        });

        setObjectReplacement("android", "bool", "config_allowAllRotations", PrefsBridge.getBoolean("system_framework_screen_all_rotations"));
    }
}
