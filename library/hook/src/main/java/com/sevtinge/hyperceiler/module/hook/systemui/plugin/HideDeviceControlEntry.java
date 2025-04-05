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
package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import static com.sevtinge.hyperceiler.module.base.BaseHook.mPrefsMap;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class HideDeviceControlEntry {
    public static void initHideDeviceControlEntry(ClassLoader classLoader) {
        if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) == 1) {
            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.external.DeviceControlEntryController", classLoader, "available", boolean.class, XC_MethodReplacement.returnConstant(true));
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) == 2) {
            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.external.DeviceControlEntryController", classLoader, "available", boolean.class, XC_MethodReplacement.returnConstant(false));
        }
    }
}
