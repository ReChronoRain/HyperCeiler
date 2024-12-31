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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BluetoothIcon extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", "updateBluetooth", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0);
                int opt_b = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth_battery", 0);
                boolean isBluetoothConnected;
                if (isAndroidVersion(34)) {
                    isBluetoothConnected = (int) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mBluetooth"), "mConnectionState") == 2;
                } else {
                    isBluetoothConnected = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBluetooth"), "isBluetoothConnected");
                }
                Object mIconController = XposedHelpers.getObjectField(param.thisObject, "mIconController");
                if (opt == 2 || (opt == 3 && !isBluetoothConnected)) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", false);
                } else if (opt == 1) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", true);
                }
                if (opt_b == 2 || (opt_b == 3 && !isBluetoothConnected)) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth_handsfree_battery", false);
                } else if (opt_b == 1) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth_handsfree_battery", true);
                }
            }
        });
    }
}
