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
package com.sevtinge.hyperceiler.ui.app.home;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;

public class HomeDockSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mDisableRecentIcon;
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
        mDisableRecentIcon.setVisible(isPad());
        mDockBackgroundBlur = findPreference("prefs_key_home_dock_bg_custom");
        mDockBackgroundColor = findPreference("prefs_key_home_dock_bg_color");
        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_home_dock_add_blur", "0"));
        mDockBackgroundBlurEnable = findPreference("prefs_key_home_dock_add_blur");
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
        mDockBackgroundColor.setVisible(mode != 2);
    }
}
