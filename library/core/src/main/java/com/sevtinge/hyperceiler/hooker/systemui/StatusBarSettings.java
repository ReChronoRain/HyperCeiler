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
package com.sevtinge.hyperceiler.hooker.systemui;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreSmallVersion;

import android.os.Bundle;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class StatusBarSettings extends DashboardFragment {
    RecommendPreference mRecommend;
    SwitchPreference mHideStatusBarOnLockScreen;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar;
    }

    @Override
    public void initPrefs() {

        mHideStatusBarOnLockScreen = findPreference("prefs_key_system_ui_status_bar_hide_icon");
        if (isMoreSmallVersion(200, 2f)) {
            mHideStatusBarOnLockScreen.setSummary(R.string.system_ui_status_bar_hide_icon_desc);
        }

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(requireContext());
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
