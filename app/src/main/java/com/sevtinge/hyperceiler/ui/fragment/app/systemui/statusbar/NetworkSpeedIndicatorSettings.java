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
package com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import androidx.preference.Preference;
import fan.preference.SeekBarPreferenceCompat;
import androidx.preference.SwitchPreference;

public class NetworkSpeedIndicatorSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    SeekBarPreferenceCompat mNetworkSpeedWidth; // 固定宽度
    SeekBarPreferenceCompat mNetworkSpeedSpacing; // 网速间间距
    SwitchPreference mNetworkSwapIcon;
    SwitchPreference mNetworkSpeedSeparator;
    SwitchPreference mNetworkAllHide;
    DropDownPreference mNetworkAlign;
    DropDownPreference mNetworkStyle;
    DropDownPreference mNetworkIcon;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_network_speed_indicator;
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
        int mNetworkMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_network_speed_style", "0"));
        mNetworkSpeedWidth = findPreference("prefs_key_system_ui_statusbar_network_speed_fixedcontent_width");
        mNetworkStyle = findPreference("prefs_key_system_ui_statusbar_network_speed_style");
        mNetworkAlign = findPreference("prefs_key_system_ui_statusbar_network_speed_align");
        mNetworkIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_icon");
        mNetworkAllHide = findPreference("prefs_key_system_ui_statusbar_network_speed_hide_all");
        mNetworkSwapIcon = findPreference("prefs_key_system_ui_statusbar_network_speed_swap_places");
        mNetworkSpeedSeparator = findPreference("prefs_key_system_ui_status_bar_no_netspeed_separator");
        mNetworkSpeedSpacing = findPreference("prefs_key_system_ui_statusbar_network_speed_spacing_margin");
        mNetworkSpeedWidth.setVisible(!isAndroidVersion(30));
        mNetworkSpeedSeparator.setVisible(!isHyperOSVersion(1f));

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
