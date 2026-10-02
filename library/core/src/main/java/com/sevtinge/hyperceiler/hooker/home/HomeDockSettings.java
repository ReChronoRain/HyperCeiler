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

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockWindowPolicy;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;

public class HomeDockSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mDisableRecentIcon;
    SwitchPreference mIconAppTitle;
    SwitchPreference mAddDockEnable;
    SwitchPreference mDockHeight;
    Preference mDockBackgroundBlur;
    DropDownPreference mDockBackgroundBlurEnable;
    DropDownPreference mDockUnlockStyle;
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
        mDockUnlockStyle = findPreference("prefs_key_home_dock_unlock_style");
        // The old keyframe mode was removed rather than retuned. Persist the one-way rename so
        // an existing installation visibly selects 重力势能 instead of retaining an orphaned value.
        String revealStyle = PrefsBridge.getString("prefs_key_home_dock_unlock_style", "daybreak");
        if ("elastic_burst".equalsIgnoreCase(revealStyle)) {
            PrefsBridge.putString("prefs_key_home_dock_unlock_style", "auto_aim");
            mDockUnlockStyle.setValue("auto_aim");
        }
        mDisableRecentIcon.setVisible(isPad());

        if (isMoreHyperOSVersion(4f)) {
            mAddDockEnable.setSummary(R.string.home_dock_os4_window_hint);
            mDockHeight.setVisible(false);
        }

        if (isPad()) {
            setFuncHint(mAddDockEnable, 1);
            setFuncHint(mIconAppTitle, 1);
            setFuncHint(mDockHeight, 1);
        }


        int mBlurMode = PrefsBridge.getStringAsInt("prefs_key_home_dock_add_blur", 0);
        mDockBackgroundBlurEnable = findPreference("prefs_key_home_dock_add_blur");

        if (isMoreHyperOSVersion(4f)) {
            mDockBackgroundBlurEnable.setEntries(R.array.home_dock_os4_styles);
            mDockBackgroundBlurEnable.setEntryValues(R.array.home_dock_os4_style_values);
            mBlurMode = DockWindowPolicy.normalizeBackgroundMode(mBlurMode);
            mDockBackgroundBlurEnable.setValue(String.valueOf(mBlurMode));
        } else if (isMoreHyperOSVersion(3f)) {
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
        mDockBackgroundBlur.setVisible(mode == 2 && !isMoreHyperOSVersion(4f));
        mDockBackgroundColor.setVisible(mode == 0 && !isMoreHyperOSVersion(4f));
    }
}
