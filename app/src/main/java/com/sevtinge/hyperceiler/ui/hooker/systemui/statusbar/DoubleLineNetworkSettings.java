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

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class DoubleLineNetworkSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    DropDownPreference mIconTheme;
    DropDownPreference mIconStyle;
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_doubleline_network;
    }

    @Override
    public void initPrefs() {
        mIconTheme = findPreference("prefs_key_system_ui_statusbar_iconmanage_mobile_network_icon_theme");
        mIconStyle = findPreference("prefs_key_system_ui_status_mobile_network_icon_style");

        //setCanBeVisible(mBlurMode);
        mIconTheme.setOnPreferenceChangeListener(this);
        mIconStyle.setVisible(mIconTheme.hashCode() == 2);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconTheme) {
            setCanBeVisible(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setCanBeVisible(int mode) {
        mIconStyle.setVisible(mode == 2);
    }
}
