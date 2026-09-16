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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class WifiStandard extends BaseHook {

    Class<?> mWifiView;
    Class<?> mWifiIconState;

    @Override
    public void init() {

        mWifiView = findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView");
        mWifiIconState = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarSignalPolicy$WifiIconState");

        if (mWifiView == null || mWifiIconState == null) {
            // Classes not found, skip hooking
            return;
        }

        findAndHookMethod(mWifiView, "applyWifiState", mWifiIconState, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Object wifiIconState = param.getArgs()[0];
                if (wifiIconState == null) {
                    return;
                }
                int mWifiStandard = com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(wifiIconState, "wifiStandard");
                int opt = PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0);
                if (opt == 1) {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField(wifiIconState, "showWifiStandard", mWifiStandard != 0);
                } else if (opt == 2) {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField(wifiIconState, "showWifiStandard", false);
                }
            }
        });
    }

    private void setWifiStandardIconState(HookParam param) {
        Object thisObj = param.getThisObject();
        if (thisObj == null) {
            return;
        }
        int wifiStandard = com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(thisObj, "wifiStandard");
        int key = PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0);
        if (key == 1) {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField(thisObj, "showWifiStandard", wifiStandard != 0);
        } else if (key == 2) {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.setBooleanField(thisObj, "showWifiStandard", false);
        }
    }
}
