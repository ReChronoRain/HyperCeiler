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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment.app.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeLayoutSettings extends SettingsPreferenceFragment {

    SwitchPreference mIconLayout;
    SwitchPreference mIconLayoutNew;
    SwitchPreference mHotseatsMarginTopSwitchPref;
    SeekBarPreferenceCompat mHotseatsMarginTopSeekPref;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_layout;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mIconLayout = findPreference("prefs_key_home_layout_unlock_grids");
        mIconLayoutNew = findPreference("prefs_key_home_layout_unlock_grids_new");
        mHotseatsMarginTopSwitchPref = findPreference("prefs_key_home_layout_hotseats_margin_top_enable");
        mHotseatsMarginTopSeekPref = findPreference("prefs_key_home_layout_hotseats_margin_top");
        if (isPad()) {
            mIconLayout.setVisible(false);
            mIconLayoutNew.setVisible(false);
        } else if (isMoreHyperOSVersion(2f)) {
            mIconLayout.setVisible(false);
            mIconLayoutNew.setVisible(true);
            mHotseatsMarginTopSwitchPref.setVisible(false);
            mHotseatsMarginTopSeekPref.setVisible(false);
        } else {
            mIconLayout.setVisible(true);
            mIconLayoutNew.setVisible(false);
        }
    }
}
