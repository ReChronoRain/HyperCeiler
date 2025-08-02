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
package com.sevtinge.hyperceiler.hooker.systemui.statusbar;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreSmallVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isSupportTelephony;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isSupportWifi;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.R;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class IconManageNewSettings extends DashboardFragment {
    DropDownPreference mNewHD;
    DropDownPreference mSmallHD;
    DropDownPreference mBigHD;

    DropDownPreference mAlarmClockIcon;
    SeekBarPreferenceCompat mAlarmClockIconN;
    SeekBarPreferenceCompat mNotificationIconMaximum;
    SwitchPreference mBatteryNumber;
    SwitchPreference mBatteryPercentage;

    SwitchPreference mHideWifiIndicator;
    DropDownPreference mHideWifi;
    DropDownPreference mHideWifiStandard;

    DropDownPreference mHideNoSIM;
    SwitchPreference mHideCard1;
    SwitchPreference mHideCard2;
    SwitchPreference mHideRoaming;
    SwitchPreference mHideVoWiFi;
    SwitchPreference mHideVoLTE;
    Preference mMobileType;
    Preference mIconMobileNetwork;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_icon_manage_new;
    }

    @Override
    public void initPrefs() {
        mSmallHD = findPreference("prefs_key_system_ui_status_bar_icon_small_hd");
        mBigHD = findPreference("prefs_key_system_ui_status_bar_icon_big_hd");
        mNewHD = findPreference("prefs_key_system_ui_status_bar_icon_new_hd");

        mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
        mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");
        mNotificationIconMaximum = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mBatteryNumber = findPreference("prefs_key_system_ui_status_bar_battery_percent");
        mBatteryPercentage = findPreference("prefs_key_system_ui_status_bar_battery_percent_mark");

        mHideWifiIndicator = findPreference("prefs_key_system_ui_status_bar_icon_wifi_network_indicator_new");
        mHideWifi = findPreference("prefs_key_system_ui_status_bar_icon_wifi");
        mHideWifiStandard = findPreference("prefs_key_system_ui_status_bar_icon_wifi_standard");

        mHideNoSIM = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_signal_no_card");
        mHideCard1 = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_hide_card_1");
        mHideCard2 = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_hide_card_2");
        mHideRoaming = findPreference("prefs_key_system_ui_status_bar_mobile_hide_roaming_icon");
        mHideVoWiFi = findPreference("prefs_key_system_ui_status_bar_icon_vowifi");
        mHideVoLTE = findPreference("prefs_key_system_ui_status_bar_icon_volte");
        mMobileType = findPreference("prefs_key_system_ui_status_bar_mobile_type");
        mIconMobileNetwork = findPreference("prefs_key_system_ui_statusbar_iconmanage_mobile_network");

        mAlarmClockIconN.setVisible(Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_status_bar_icon_alarm_clock", "0")) == 3);

        if (isMoreSmallVersion(200, 2f)) {
            setFuncHint(mSmallHD, 1);
            setFuncHint(mBigHD, 1);
            setFuncHint(mNewHD, 1);
        }

        if (getContext() != null) {

            if (!isSupportWifi(getContext())) {
               setHide(mHideWifiIndicator, false);
               setHide(mHideWifi, false);
               setHide(mHideWifiStandard, false);
            }

            if (!isSupportTelephony(getContext())) {
                setHide(mSmallHD, false);
                setHide(mBigHD, false);
                setHide(mNewHD, false);
                setHide(mHideNoSIM, false);
                setHide(mHideCard1, false);
                setHide(mHideCard2, false);
                setHide(mHideRoaming, false);
                setHide(mHideVoWiFi, false);
                setHide(mHideVoLTE, false);
                setHide(mMobileType, false);
                setHide(mIconMobileNetwork, false);
            }

        }

        mAlarmClockIcon.setOnPreferenceChangeListener((preference, o) -> {
            mAlarmClockIconN.setVisible(Integer.parseInt((String) o) == 3);
            return true;
        });

        mNotificationIconMaximum.setOnPreferenceChangeListener((preference, o) -> {
            if ((int) o == 16) {
                mNotificationIconMaximum.setValue(R.string.unlimited);
            }
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
