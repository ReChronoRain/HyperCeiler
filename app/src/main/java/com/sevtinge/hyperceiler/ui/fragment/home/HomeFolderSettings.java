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
package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.os.Build;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class HomeFolderSettings extends SettingsPreferenceFragment {

    DropDownPreference mFolderShade;
    SeekBarPreferenceEx mFolderShadeLevel;

    SeekBarPreferenceEx mFolderColumns;
    SwitchPreference mFolderWidth;
    SwitchPreference mFolderSpace;
    SwitchPreference mUnlockFolderBlur;
    SwitchPreference mUnlockFolderBlurSupport;
    Preference mSmallFolderIconBackgroundCustom;
    Preference mSmallFolderIconBackgroundCustom1;
    Preference mSmallFolderIconBackgroundCustom2;
    Preference mSmallFolderIconBackgroundCustom3;

    @Override
    public int getContentResId() {
        return R.xml.home_folder;
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
        mFolderShade = findPreference("prefs_key_home_folder_shade");
        mFolderShadeLevel = findPreference("prefs_key_home_folder_shade_level");
        mUnlockFolderBlur = findPreference("prefs_key_home_folder_blur");
        mUnlockFolderBlurSupport = findPreference("prefs_key_home_folder_unlock_blur_supported");

        mUnlockFolderBlur.setVisible(!isAndroidVersion(30));
        mUnlockFolderBlurSupport.setVisible(!isAndroidVersion(30) && isMoreHyperOSVersion(1f));

        mFolderColumns = findPreference("prefs_key_home_folder_columns");
        mFolderWidth = findPreference("prefs_key_home_folder_width");
        mFolderSpace = findPreference("prefs_key_home_folder_space");
        mSmallFolderIconBackgroundCustom = findPreference("prefs_key_home_small_folder_icon_bg_custom");
        if (mSmallFolderIconBackgroundCustom != null) {
            mSmallFolderIconBackgroundCustom.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.S));
            mSmallFolderIconBackgroundCustom.setEnabled(mSmallFolderIconBackgroundCustom.isVisible());
        }

        mSmallFolderIconBackgroundCustom1 = findPreference("prefs_key_home_big_folder_icon_bg_2x1");
        if (mSmallFolderIconBackgroundCustom1 != null) {
            mSmallFolderIconBackgroundCustom1.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.S));
            mSmallFolderIconBackgroundCustom1.setEnabled(mSmallFolderIconBackgroundCustom1.isVisible());
        }
        mSmallFolderIconBackgroundCustom2 = findPreference("prefs_key_home_big_folder_icon_bg_1x2");
        if (mSmallFolderIconBackgroundCustom2 != null) {
            mSmallFolderIconBackgroundCustom2.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.S));
            mSmallFolderIconBackgroundCustom2.setEnabled(mSmallFolderIconBackgroundCustom2.isVisible());
        }
        mSmallFolderIconBackgroundCustom3 = findPreference("prefs_key_home_big_folder_icon_bg");
        if (mSmallFolderIconBackgroundCustom3 != null) {
            mSmallFolderIconBackgroundCustom3.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.S));
            mSmallFolderIconBackgroundCustom3.setEnabled(mSmallFolderIconBackgroundCustom3.isVisible());
        }

        setBigFolderTextForPad();
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

    private void setBigFolderTextForPad(){
        if (isPad()){
            mSmallFolderIconBackgroundCustom1.setTitle(R.string.home_big_folder_icon_bg_2x1_n);
            mSmallFolderIconBackgroundCustom2.setTitle(R.string.home_big_folder_icon_bg_1x2_n);
            mSmallFolderIconBackgroundCustom3.setTitle(R.string.home_big_folder_icon_bg_n);
        }
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
