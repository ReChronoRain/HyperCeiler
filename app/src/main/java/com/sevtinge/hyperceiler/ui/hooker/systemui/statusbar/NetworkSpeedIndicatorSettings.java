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
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class NetworkSpeedIndicatorSettings extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    SeekBarPreferenceCompat mNetworkSpeedSpacing; // 网速间间距
    SwitchPreference mNetworkSwapIcon;
    SwitchPreference mNetworkAllHide;
    DropDownPreference mNetworkAlign;
    DropDownPreference mNetworkStyle;
    DropDownPreference mNetworkIcon;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_network_speed_indicator;
    }

    @Override
    public void initPrefs() {
        int mNetworkMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_network_speed_style", "0"));
        mNetworkStyle = findPreference("prefs_key_system_ui_statusbar_network_speed_style");
        mNetworkAlign = findPreference("prefs_key_system_ui_statusbar_network_speed_align");
        mNetworkIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_icon");
        mNetworkAllHide = findPreference("prefs_key_system_ui_statusbar_network_speed_hide_all");
        mNetworkSwapIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_swap_places");
        mNetworkSpeedSpacing = findPreference("prefs_key_system_ui_statusbar_network_speed_spacing_margin");

        setNetworkMode(mNetworkMode);
        mNetworkStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mNetworkStyle) {
            setNetworkMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setNetworkMode(int mode) {
        mNetworkIcon.setVisible(mode == 3 || mode == 4);
        mNetworkSwapIcon.setVisible(mode == 3 || mode == 4);
        mNetworkAllHide.setVisible(mode == 3 || mode == 4);
        mNetworkAlign.setVisible(mode == 2 || mode == 4);
        mNetworkSpeedSpacing.setVisible(mode == 2 || mode == 4);
    }
}
