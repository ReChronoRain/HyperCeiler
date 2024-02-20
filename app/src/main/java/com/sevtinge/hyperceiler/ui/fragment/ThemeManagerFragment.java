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
package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class ThemeManagerFragment extends SettingsPreferenceFragment {

    PreferenceCategory mVersionCodeModifyPreferenceCat;
    SwitchPreference mVersionCodeModifyPreference;

    @Override
    public int getContentResId() {
        return R.xml.theme_manager;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.theme_manager),
            "com.android.thememanager"
        );
    }

    @Override
    public void initPrefs() {
        mVersionCodeModifyPreferenceCat = findPreference("prefs_key_theme_manager_version_code_modify_cat");
        mVersionCodeModifyPreference = findPreference("prefs_key_theme_manager_version_code_modify");

        if (!isMoreMiuiVersion(13f)) {
            mVersionCodeModifyPreferenceCat.setVisible(!isMoreHyperOSVersion(1f));
        } else {
            mVersionCodeModifyPreferenceCat.setVisible(false);
            mVersionCodeModifyPreference.setChecked(false);
            mVersionCodeModifyPreference.setEnabled(false);
        }
    }
}
