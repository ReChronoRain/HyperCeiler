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
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeGestureSettings extends DashboardFragment {

    SwitchPreference mQuickBack;
    SwitchPreference mDisableAllGesture;
    SeekBarPreferenceCompat mHighBackArea;
    SeekBarPreferenceCompat mWideBackArea;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_gesture;
    }

    @Override
    public void initPrefs() {
        mQuickBack = findPreference("prefs_key_home_navigation_quick_back");
        mDisableAllGesture = findPreference("prefs_key_home_navigation_disable_full_screen_back_gesture");
        mHighBackArea = findPreference("prefs_key_home_navigation_back_area_height");
        mWideBackArea = findPreference("prefs_key_home_navigation_back_area_width");

        boolean mSwitch = getSharedPreferences().getBoolean(mDisableAllGesture.getKey(), false);

        if (isPad()) {
            setFuncHint(mDisableAllGesture, 1);
        } else if (isMoreHyperOSVersion(3f)) {
            mQuickBack.setVisible(false);
            mHighBackArea.setEnabled(mSwitch);
            mWideBackArea.setEnabled(mSwitch);
        }

        mDisableAllGesture.setOnPreferenceChangeListener(
            (v, newValue) -> {
                boolean enabled = (Boolean) newValue;
                mHighBackArea.setEnabled(enabled);
                mWideBackArea.setEnabled(enabled);
                return true;
            }
        );
    }
}
