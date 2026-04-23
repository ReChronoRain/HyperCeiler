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

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import java.util.Objects;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class HomeGestureSettings extends DashboardFragment {

    SwitchPreference mQuickBack;
    SwitchPreference mGestureEnable;
    SwitchPreference mDisableAllGesture;
    DropDownPreference mBackGestureHaptic;
    SeekBarPreferenceCompat mHighBackArea;
    SeekBarPreferenceCompat mWideBackArea;
    PreferenceCategory mGestureActions;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_gesture;
    }

    @Override
    public void initPrefs() {
        mGestureEnable = findPreference("prefs_key_home_gesture_enable");
        mQuickBack = findPreference("prefs_key_home_navigation_quick_back");
        mDisableAllGesture = findPreference("prefs_key_home_navigation_disable_full_screen_back_gesture");
        mBackGestureHaptic = findPreference("prefs_key_home_gesture_back_haptic");
        mHighBackArea = findPreference("prefs_key_home_navigation_back_area_height");
        mWideBackArea = findPreference("prefs_key_home_navigation_back_area_width");
        mGestureActions = findPreference("prefs_key_home_gesture_actions");

        boolean mSwitch = getSharedPreferences().getBoolean(mDisableAllGesture.getKey(), false);
        boolean gesturesEnabled = getSharedPreferences().getBoolean(mGestureEnable.getKey(), false);

        if (mGestureActions != null) {
            mGestureActions.setVisible(gesturesEnabled);
        }

        if (isPad()) {
            setFuncHint(mBackGestureHaptic, 1);
            setFuncHint(mDisableAllGesture, 1);
        } else if (isMoreHyperOSVersion(3f)) {
            mQuickBack.setVisible(false);
            mHighBackArea.setEnabled(mSwitch);
            mWideBackArea.setEnabled(mSwitch);
            updateBackGestureHapticSummary(getSharedPreferences().getString("prefs_key_home_gesture_back_haptic", "0"));
        }

        mDisableAllGesture.setOnPreferenceChangeListener(
            (v, newValue) -> {
                boolean enabled = (Boolean) newValue;
                mHighBackArea.setEnabled(enabled);
                mWideBackArea.setEnabled(enabled);
                return true;
            }
        );

        mGestureEnable.setOnPreferenceChangeListener(
            (v, newValue) -> {
                if (mGestureActions != null) {
                    mGestureActions.setVisible((Boolean) newValue);
                }
                return true;
            }
        );

        if (mBackGestureHaptic != null && !isPad()) {
            mBackGestureHaptic.setOnPreferenceChangeListener((preference, newValue) -> {
                updateBackGestureHapticSummary((String) newValue);
                return true;
            });
        }
    }

    private void updateBackGestureHapticSummary(String value) {
        if (mBackGestureHaptic == null) {
            return;
        }
        if (Objects.equals(value, "1")) {
            mBackGestureHaptic.setSummary(R.string.home_gesture_back_haptic_enhanced_tips);
        } else if (Objects.equals(value, "2")) {
            mBackGestureHaptic.setSummary("");
        } else {
            mBackGestureHaptic.setSummary(R.string.home_gesture_back_haptic_default_tips);
        }
    }
}
