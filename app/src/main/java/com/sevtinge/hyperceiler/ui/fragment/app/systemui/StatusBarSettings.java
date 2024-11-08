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
package com.sevtinge.hyperceiler.ui.fragment.app.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class StatusBarSettings extends SettingsPreferenceFragment {

    Preference mClockStatus; // 时钟指示器
    Preference mDeviceStatus; // 硬件指示器
    Preference mToastStatus; // 灵动 Toast
    Preference mIconManager;
    PreferenceCategory mStatusBarLayout; // 状态栏布局
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mIconManager = findPreference("prefs_key_icon_manager");
        mClockStatus = findPreference("prefs_key_clock_status");

        mDeviceStatus = findPreference("prefs_key_system_ui_status_bar_device");
        mToastStatus = findPreference("prefs_key_system_ui_status_bar_toast");
        mStatusBarLayout = findPreference("pref_key_system_ui_statusbar_layout");
        mDeviceStatus.setVisible(!isMoreHyperOSVersion(1f) || !isMoreAndroidVersion(34));
        mToastStatus.setVisible(isMoreHyperOSVersion(1f));

        if (isMoreHyperOSVersion(1f)) {
            mIconManager.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageNewSettings");
            mClockStatus.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.NewClockIndicatorSettings");
        } else {
            mIconManager.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageSettings");
            mClockStatus.setFragment("com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.ClockIndicatorSettings");
        }

        mStatusBarLayout.setVisible(!isMoreHyperOSVersion(1f));

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_system_ui_lock_screen_hide_status_bar");
        mRecommend.addRecommendView(getString(R.string.system_ui_lock_screen_hide_status_bar),
                null,
                LockScreenSettings.class,
                args1,
                R.string.system_ui_lockscreen_title
        );

    }
}
