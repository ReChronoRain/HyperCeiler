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
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.ALL_APPS_MODE;

import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

import fan.preference.SeekBarPreferenceCompat;

public class HomeRecentSettings extends DashboardFragment {

    Preference mHideRecentCard;
    SeekBarPreferenceCompat mTaskViewHeight;
    SwitchPreference mShowLaunch;
    SwitchPreference mHideWorldCirculate;
    SwitchPreference mHideFreeform;
    SwitchPreference mUnlockPin;
    SwitchPreference mShowMenInfo;
    SwitchPreference mHideCleanIcon;
    SwitchPreference mNotHideCleanIcon;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_recent;
    }

    @Override
    public void initPrefs() {
        mShowLaunch = findPreference("prefs_key_home_recent_show_launch");
        mHideWorldCirculate = findPreference("prefs_key_home_recent_hide_world_circulate");
        mHideFreeform = findPreference("prefs_key_home_recent_hide_freeform");
        mUnlockPin = findPreference("prefs_key_home_recent_unlock_pin");
        mHideRecentCard = findPreference("prefs_key_home_recent_hide_card");
        mTaskViewHeight = findPreference("prefs_key_home_recent_task_view_height");
        mShowMenInfo = findPreference("prefs_key_home_recent_show_memory_info");
        mHideCleanIcon = findPreference("prefs_key_home_recent_hide_clean_up");
        mNotHideCleanIcon = findPreference("prefs_key_always_show_clean_up");

        mTaskViewHeight.setVisible(isPad());
        mShowMenInfo.setVisible(isPad());

        if (isMoreHyperOSVersion(3f)) {
            setFuncHint(mShowLaunch, 1);
            setFuncHint(mHideWorldCirculate, isPad() ? 1 : 2);
            setFuncHint(mHideFreeform, 1);
            setHide(mUnlockPin, false);
        }

        mHideRecentCard.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                    intent.putExtra("mode", ALL_APPS_MODE);
                    intent.putExtra("key", preference.getKey());
                    startActivity(intent);
                    return true;
                }
        );

        mHideCleanIcon.setOnPreferenceChangeListener((preference, o) -> {
            if (!(boolean) o) {
                mNotHideCleanIcon.setChecked(false);
            }
            return true;
        });
    }
}
