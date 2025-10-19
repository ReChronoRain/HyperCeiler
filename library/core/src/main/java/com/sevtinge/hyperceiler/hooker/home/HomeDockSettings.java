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

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.core.R;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;

public class HomeDockSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mDisableRecentIcon;
    SwitchPreference mIconAppTitle;
    SwitchPreference mAddDockEnable;
    SwitchPreference mDockHeight;
    Preference mDockBackgroundBlur;
    DropDownPreference mDockBackgroundBlurEnable;
    ColorPickerPreference mDockBackgroundColor;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_dock;
    }

    @Override
    public void initPrefs() {
        mDisableRecentIcon = findPreference("prefs_key_home_dock_disable_recents_icon");
        mDockBackgroundBlur = findPreference("prefs_key_home_dock_bg_custom");
        mDockHeight = findPreference("prefs_key_home_dock_bg_all_app");
        mDockBackgroundColor = findPreference("prefs_key_home_dock_bg_color");
        mAddDockEnable = findPreference("prefs_key_home_dock_bg_custom_enable");
        mIconAppTitle = findPreference("prefs_key_home_dock_icon_title");
        mDisableRecentIcon.setVisible(isPad());

        if (isPad()) {
            setFuncHint(mAddDockEnable, 1);
            setFuncHint(mIconAppTitle, 1);
            setFuncHint(mDockHeight, 1);
        }


        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_home_dock_add_blur", "0"));
        mDockBackgroundBlurEnable = findPreference("prefs_key_home_dock_add_blur");

        if (isMoreHyperOSVersion(3f)) {
            if (mBlurMode == 2) {
                cleanKey(mDockBackgroundBlurEnable.getKey());
            }

            mDockBackgroundBlurEnable.setEntries(R.array.blur_switch_new);
            mDockBackgroundBlurEnable.setEntryValues(R.array.blur_switch_value_new);
        }

        setCanBeVisible(mBlurMode);
        mDockBackgroundBlurEnable.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mDockBackgroundBlurEnable) {
            setCanBeVisible(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setCanBeVisible(int mode) {
        mDockBackgroundBlur.setVisible(mode == 2);
        mDockBackgroundColor.setVisible(mode == 0);
    }
}
