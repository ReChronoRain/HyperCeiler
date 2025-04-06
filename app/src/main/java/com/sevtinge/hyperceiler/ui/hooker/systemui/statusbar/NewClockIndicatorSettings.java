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
package com.sevtinge.hyperceiler.ui.hooker.systemui.statusbar;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

public class NewClockIndicatorSettings extends DashboardFragment {

    SwitchPreference mClockAnim;
    PreferenceCategory mPadClock;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_new_clock_indicator;
    }

    @Override
    public void initPrefs() {
        mClockAnim = findPreference("prefs_key_system_ui_disable_clock_anim");
        mPadClock = findPreference("prefs_key_system_ui_statusbar_clock_pad_show");

        mClockAnim.setVisible(!isMoreHyperOSVersion(2f));
        mPadClock.setVisible(isPad());
    }
}
