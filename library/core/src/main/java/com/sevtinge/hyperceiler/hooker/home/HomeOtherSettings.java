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
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.core.R;

public class HomeOtherSettings extends DashboardFragment {

    SwitchPreference mMoveToMinusOneScreen;
    SwitchPreference mWindowedMode;
    SwitchPreference mShareAPK;
    SwitchPreference mEnableMoreSettings;
    SwitchPreference mHideReportText;
    SwitchPreference mDisablePreLoad;


    @Override
    public int getPreferenceScreenResId() {
        if (isMoreHyperOSVersion(3f)) {
            return R.xml.home_other_new;
        }
        return R.xml.home_other;
    }

    @Override
    public void initPrefs() {
        if (isMoreHyperOSVersion(3f)) {
            mMoveToMinusOneScreen = findPreference("prefs_key_home_widget_allow_moved_to_minus_one_screen");
            if (isPad()) setFuncHint(mMoveToMinusOneScreen, 1);
        }

        mWindowedMode = findPreference("prefs_key_home_other_freeform_shortcut_menu");
        mShareAPK = findPreference("prefs_key_home_other_allow_share_apk");
        mHideReportText = findPreference("prefs_key_home_title_hide_report_text");
        mDisablePreLoad = findPreference("prefs_key_home_other_disable_prestart");

        if (isPad()) {
            setFuncHint(mWindowedMode, 2);
            setFuncHint(mShareAPK, 1);
            setFuncHint(mHideReportText, 1);
            setFuncHint(mDisablePreLoad, 1);
        }

        mEnableMoreSettings = findPreference("prefs_key_home_other_mi_pad_enable_more_setting");
        mEnableMoreSettings.setVisible(isPad() && !isMoreHyperOSVersion(3f));
    }

}
