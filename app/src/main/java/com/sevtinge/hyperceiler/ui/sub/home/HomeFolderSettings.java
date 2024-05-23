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
package com.sevtinge.hyperceiler.ui.sub.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;
import fan.preference.SwitchPreference;

public class HomeFolderSettings extends SettingsPreferenceFragment {

    DropDownPreference mFolderShade;
    SeekBarPreferenceCompat mFolderShadeLevel;

    SeekBarPreferenceCompat mFolderColumns;
    SwitchPreference mFolderWidth;
    SwitchPreference mFolderSpace;
    SwitchPreference mUnlockFolderBlurSupport;
    SwitchPreference mSmallFolderIconBackgroundCustom1;
    SwitchPreference mSmallFolderIconBackgroundCustom2;
    SwitchPreference mSmallFolderIconBackgroundCustom3;

    @Override
    public int getContentResId() {
        return R.xml.home_folder;
    }

    @Override
    public void initPrefs() {
        mFolderShade = findPreference("prefs_key_home_folder_shade");
        mFolderShadeLevel = findPreference("prefs_key_home_folder_shade_level");
        mUnlockFolderBlurSupport = findPreference("prefs_key_home_folder_unlock_blur_supported");

        mFolderColumns = findPreference("prefs_key_home_folder_columns");
        mFolderWidth = findPreference("prefs_key_home_folder_width");
        mFolderSpace = findPreference("prefs_key_home_folder_space");

        if (isPad()) {
            mSmallFolderIconBackgroundCustom1 = findPreference("prefs_key_home_big_folder_icon_bg_2x1");
            mSmallFolderIconBackgroundCustom2 = findPreference("prefs_key_home_big_folder_icon_bg_1x2");
            mSmallFolderIconBackgroundCustom3 = findPreference("prefs_key_home_big_folder_icon_bg");

            mSmallFolderIconBackgroundCustom1.setTitle(R.string.home_big_folder_icon_bg_2x1_n);
            mSmallFolderIconBackgroundCustom2.setTitle(R.string.home_big_folder_icon_bg_1x2_n);
            mSmallFolderIconBackgroundCustom3.setTitle(R.string.home_big_folder_icon_bg_n);
        }
        mUnlockFolderBlurSupport.setVisible(isMoreHyperOSVersion(1f));
        setFolderShadeLevelEnable(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_home_folder_shade", "0")));
        setFolderWidthEnable(PrefsUtils.mSharedPreferences.getInt(mFolderColumns.getKey(), 3));
        setFolderSpaceEnable(PrefsUtils.mSharedPreferences.getInt(mFolderColumns.getKey(), 3));

        mFolderShade.setOnPreferenceChangeListener((preference, o) -> {
            setFolderShadeLevelEnable(Integer.parseInt((String) o));
            return true;
        });

        mFolderColumns.setOnPreferenceChangeListener(((preference, o) -> {
            setFolderWidthEnable((Integer) o);
            setFolderSpaceEnable((Integer) o);
            return true;
        }));
    }

    private void setFolderShadeLevelEnable(int i) {
        boolean isEnable = i != 0;
        mFolderShadeLevel.setVisible(isEnable);
        mFolderShadeLevel.setEnabled(isEnable);
    }

    private void setFolderWidthEnable(int columns) {
        boolean isEnable = columns > 1;
        mFolderWidth.setVisible(isEnable);
        mFolderWidth.setEnabled(isEnable);
    }

    private void setFolderSpaceEnable(int columns) {
        boolean isEnable = columns > 3;
        mFolderSpace.setVisible(isEnable);
        mFolderSpace.setEnabled(isEnable);
    }
}
