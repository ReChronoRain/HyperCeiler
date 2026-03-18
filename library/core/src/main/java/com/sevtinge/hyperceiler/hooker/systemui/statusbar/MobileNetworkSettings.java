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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class MobileNetworkSettings extends DashboardFragment {

    DropDownPreference mNewHD;
    DropDownPreference mSmallHD;
    DropDownPreference mBigHD;
    DropDownPreference mShowSignal;
    SwitchPreference mHideCard1;
    SwitchPreference mHideCard2;
    Preference mDoubleMobile;

    int mSignalMode;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_mobile;
    }

    @Override
    public void initPrefs() {
        mSignalMode = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_status_bar_icon_mobile_network_signal_mode", "0"));

        mSmallHD = findPreference("prefs_key_system_ui_status_bar_icon_small_hd");
        mBigHD = findPreference("prefs_key_system_ui_status_bar_icon_big_hd");
        mNewHD = findPreference("prefs_key_system_ui_status_bar_icon_new_hd");
        mShowSignal = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_signal_mode");
        mHideCard1 = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_hide_card_1");
        mHideCard2 = findPreference("prefs_key_system_ui_status_bar_icon_mobile_network_hide_card_2");
        mDoubleMobile = findPreference("prefs_key_system_ui_statusbar_iconmanage_mobile_network");

        if (isMoreHyperOSVersion(3f)) {
            setPreVisible(mSmallHD, false);
            setPreVisible(mBigHD, false);
            setPreVisible(mNewHD, false);
        } else if (isMoreSmallVersion(200, 2f)) {
            setFuncHint(mSmallHD, 1);
            setFuncHint(mBigHD, 1);
            setFuncHint(mNewHD, 1);
        }

        updateMobileState(mSignalMode, mHideCard1.isChecked(), mHideCard2.isChecked());

        mHideCard1.setOnPreferenceChangeListener((preference, newValue) -> {
            updateMobileState(mSignalMode, (boolean) newValue, mHideCard2.isChecked());
            return true;
        });

        mHideCard2.setOnPreferenceChangeListener((preference, newValue) -> {
            updateMobileState(mSignalMode, mHideCard1.isChecked(), (boolean) newValue);
            return true;
        });

        mShowSignal.setOnPreferenceChangeListener((preference, newValue) -> {
            mSignalMode = Integer.parseInt((String) newValue);
            updateMobileState(mSignalMode, mHideCard1.isChecked(), mHideCard2.isChecked());
            return true;
        });
    }

    private void updateMobileState(int signalMode, boolean card1Checked, boolean card2Checked) {
        if (signalMode >= 2) {
            mHideCard1.setEnabled(false);
            mHideCard1.setChecked(false);
            mHideCard1.setSummary(R.string.system_ui_statusbar_iconmanage_nosupport_mobile_network);
            mHideCard2.setEnabled(false);
            mHideCard2.setChecked(false);
            mHideCard2.setSummary(R.string.system_ui_statusbar_iconmanage_nosupport_mobile_network);
            mDoubleMobile.setEnabled(false);
            mDoubleMobile.setSummary(R.string.system_ui_statusbar_iconmanage_nosupport_mobile_network);
        } else {
            mHideCard1.setEnabled(true);
            mHideCard1.setSummary("");
            mHideCard2.setEnabled(true);
            mHideCard2.setSummary("");
            if (signalMode == 1 || card1Checked || card2Checked) {
                mDoubleMobile.setEnabled(false);
                mDoubleMobile.setSummary(R.string.system_ui_statusbar_iconmanage_nosupport_mobile_network);
            } else {
                mDoubleMobile.setEnabled(true);
                mDoubleMobile.setSummary("");
            }
        }
    }
}
