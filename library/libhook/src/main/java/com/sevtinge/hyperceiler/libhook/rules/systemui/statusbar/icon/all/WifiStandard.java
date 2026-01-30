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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class WifiStandard extends BaseHook {

    Class<?> mWifiView;
    Class<?> mWifiIconState;

    @Override
    public void init() {

        mWifiView = findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView");
        mWifiIconState = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarSignalPolicy$WifiIconState");


        findAndHookMethod(mWifiView, "applyWifiState", mWifiIconState, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Object mWifiIconState = param.getArgs()[0];
                int mWifiStandard = EzxHelpUtils.getIntField(mWifiIconState, "wifiStandard");
                if (mWifiIconState != null) {
                    int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0);
                    if (opt == 1) {
                        EzxHelpUtils.setBooleanField(mWifiIconState, "showWifiStandard", mWifiStandard != 0);
                    } else if (opt == 2) {
                        EzxHelpUtils.setBooleanField(mWifiIconState, "showWifiStandard", false);
                    }
                }
            }
        });
    }

    private void setWifiStandardIconState(BeforeHookParam param) {
        int wifiStandard = EzxHelpUtils.getIntField(param.getThisObject(), "wifiStandard");
        int key = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0);
        if (key == 1) {
            EzxHelpUtils.setBooleanField(param.getThisObject(), "showWifiStandard", wifiStandard != 0);
        } else if (key == 2) {
            EzxHelpUtils.setBooleanField(param.getThisObject(), "showWifiStandard", false);
        }
    }
}
