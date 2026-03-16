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
package com.sevtinge.hyperceiler.hooker.systemui.statusbar;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isSupportTelephony;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isSupportWifi;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class IconManageNewSettings extends DashboardFragment {

    DropDownPreference mAlarmClockIcon;
    SeekBarPreferenceCompat mAlarmClockIconN;
    SwitchPreference mBatteryNumber;
    SwitchPreference mBatteryPercentage;

    SwitchPreference mHideWifiIndicator;
    DropDownPreference mHideWifi;
    DropDownPreference mHideWifiStandard;
    SwitchPreference mSwitchSwap;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_icon_manage_new;
    }

    @Override
    public void initPrefs() {
        mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
        mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");

        mBatteryNumber = findPreference("prefs_key_system_ui_status_bar_battery_percent");
        mBatteryPercentage = findPreference("prefs_key_system_ui_status_bar_battery_percent_mark");

        mHideWifiIndicator = findPreference("prefs_key_system_ui_status_bar_icon_wifi_network_indicator_new");
        mHideWifi = findPreference("prefs_key_system_ui_status_bar_icon_wifi");
        mHideWifiStandard = findPreference("prefs_key_system_ui_status_bar_icon_wifi_standard");

        mSwitchSwap = findPreference("prefs_key_system_ui_status_bar_swap_wifi_and_mobile_network");

        mAlarmClockIconN.setVisible(Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_status_bar_icon_alarm_clock", "0")) == 3);

        if (getContext() != null) {
            if (!isSupportWifi(getContext())) {
               setPreVisible(mHideWifiIndicator, false);
               setPreVisible(mHideWifi, false);
               setPreVisible(mHideWifiStandard, false);
            }

            if (!isSupportTelephony(getContext())) {
                setPreVisible(mSwitchSwap, false);
            }

        }

        mAlarmClockIcon.setOnPreferenceChangeListener((preference, o) -> {
            mAlarmClockIconN.setVisible(Integer.parseInt((String) o) == 3);
            return true;
        });

        mBatteryNumber.setOnPreferenceChangeListener((preference, o) -> {
            if (!(boolean) o) {
                mBatteryPercentage.setChecked(false);
            }
            return true;
        });
    }
}
