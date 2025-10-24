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

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.core.R;

import java.util.function.BiConsumer;

import fan.preference.SeekBarPreferenceCompat;

public class HomeLayoutSettings extends DashboardFragment {

    PreferenceCategory mSearch;
    SwitchPreference mIconLayoutNew;
    SwitchPreference mLayoutH;
    SwitchPreference mWidth;
    SwitchPreference mPadding;
    SeekBarPreferenceCompat mPhone;
    SeekBarPreferenceCompat mPadH;
    SeekBarPreferenceCompat mPadV;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_layout;
    }

    @Override
    public void initPrefs() {
        boolean mWidthEnable = getSharedPreferences().getBoolean("prefs_key_home_folder_width", false);
        boolean mPaddingEnable = getSharedPreferences().getBoolean("prefs_key_home_folder_horizontal_padding_enable", false);

        mSearch = findPreference("prefs_key_home_layout_searchbar_title");
        mIconLayoutNew = findPreference("prefs_key_home_layout_unlock_grids_new");
        mLayoutH = findPreference("prefs_key_home_layout_workspace_padding_horizontal_enable");
        mWidth = findPreference("prefs_key_home_folder_width");
        mPadding = findPreference("prefs_key_home_folder_horizontal_padding_enable");
        mPhone = findPreference("prefs_key_home_folder_horizontal_padding");
        mPadH = findPreference("prefs_key_home_folder_horizontal_padding_pad_h");
        mPadV = findPreference("prefs_key_home_folder_horizontal_padding_pad_v");

        if (isPad()) {
            setHide(mSearch, false);
            setFuncHint(mLayoutH, 1);
            setFuncHint(mIconLayoutNew, 1);
        }

        BiConsumer<Boolean, Boolean> updateVisibility = (widthEnabled, paddingEnabled) -> {
            mPadding.setVisible(widthEnabled);
            if (isPad()) {
                boolean showPad = widthEnabled && paddingEnabled;
                mPadH.setVisible(showPad);
                mPadV.setVisible(showPad);
                mPhone.setVisible(false);
            } else {
                mPhone.setVisible(widthEnabled && paddingEnabled);
                mPadH.setVisible(false);
                mPadV.setVisible(false);
            }
        };

        updateVisibility.accept(mWidthEnable, mPaddingEnable);

        mWidth.setOnPreferenceChangeListener((preference, o) -> {
            boolean newWidth = (boolean) o;
            boolean curPaddingEnable = getSharedPreferences().getBoolean("prefs_key_home_folder_horizontal_padding_enable", false);
            updateVisibility.accept(newWidth, curPaddingEnable);
            return true;
        });

        mPadding.setOnPreferenceChangeListener((p, v) -> {
            boolean newPadding = (boolean) v;
            boolean curWidth = getSharedPreferences().getBoolean("prefs_key_home_folder_width", false);
            updateVisibility.accept(curWidth, newPadding);
            return true;
        });
    }
}
