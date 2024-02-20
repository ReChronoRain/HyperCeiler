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
package com.sevtinge.hyperceiler.ui.fragment.systemui;

import android.provider.Settings;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class NavigationSettings extends SettingsPreferenceFragment {
    SwitchPreference customNav;
    PreferenceCategory mNav;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_navigation;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    private boolean isGestureNavigationEnabled() {
        var defaultNavigationMode = 0;
        var gestureNavigationMode = 2;

        return Settings.Secure.getInt(requireContext().getContentResolver(), "navigation_mode", defaultNavigationMode) == gestureNavigationMode;
    }

    @Override
    public void initPrefs() {
        customNav = findPreference("prefs_key_system_ui_navigation_custom");
        mNav = findPreference("prefs_key_system_ui_navigation");
        if (customNav != null) {
            mNav.setEnabled(!isGestureNavigationEnabled());
            mNav.setVisible(mNav.isEnabled());
        }

    }
}
