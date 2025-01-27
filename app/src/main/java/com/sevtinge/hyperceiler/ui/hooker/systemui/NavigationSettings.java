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
package com.sevtinge.hyperceiler.ui.hooker.systemui;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.KillApp;

public class NavigationSettings extends DashboardFragment {
    SwitchPreference navigation;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_navigation;
    }

    @Override
    public void initPrefs() {
        navigation = findPreference("prefs_key_system_ui_hide_navigation_bar");
        navigation.setOnPreferenceChangeListener((preference, o) -> {
            KillApp.killApps("com.miui.home", "com.android.systemui");
            return true;
        });
    }
}
