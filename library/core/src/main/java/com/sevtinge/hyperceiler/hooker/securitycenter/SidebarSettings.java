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
package com.sevtinge.hyperceiler.hooker.securitycenter;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class SidebarSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    SwitchPreference mHideSideBar;
    SwitchPreference mSideBarColor;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.security_center_sidebar;
    }

    @Override
    public void initPrefs() {
        boolean isEnableHideLine = getSharedPreferences().getBoolean("prefs_key_security_center_hide_sidebar", false);
        mHideSideBar = findPreference("prefs_key_security_center_hide_sidebar");
        mSideBarColor = findPreference("prefs_key_security_center_sidebar_line_color");

        setMode(isEnableHideLine);
        mHideSideBar.setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference == mHideSideBar) {
            setMode((Boolean) newValue);
        }
        return true;
    }

    private void setMode(boolean mode) {
        if (mode) {
            mSideBarColor.setChecked(false);
            mSideBarColor.setVisible(false);
        } else {
            mSideBarColor.setVisible(true);
        }
    }
}
