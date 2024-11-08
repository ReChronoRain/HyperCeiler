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

import android.content.Intent;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import androidx.preference.Preference;

public class BatteryIndicatorSettings extends SettingsPreferenceFragment {

    DropDownPreference mBatteryIndicatorColor;
    ColorPickerPreference mBatteryIndicatorFullPower;
    ColorPickerPreference mBatteryIndicatorLowPower;
    ColorPickerPreference mBatteryIndicatorPowerSaving;
    ColorPickerPreference mBatteryIndicatorPowerCharging;
    Preference mBatteryIndicatorTest;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_battery_indicator;
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

        mBatteryIndicatorColor = findPreference("prefs_key_system_ui_status_bar_battery_indicator_color");
        mBatteryIndicatorFullPower = findPreference("prefs_key_system_ui_status_bar_battery_indicator_color_full_power");
        mBatteryIndicatorLowPower = findPreference("prefs_key_system_ui_status_bar_battery_indicator_color_low_power");
        mBatteryIndicatorPowerSaving = findPreference("prefs_key_system_ui_status_bar_battery_indicator_color_power_saving");
        mBatteryIndicatorPowerCharging = findPreference("prefs_key_system_ui_status_bar_battery_indicator_color_power_charging");

        showBatteryIndicatorColor(Integer.parseInt(PrefsUtils.getSharedStringPrefs(getActivity(), mBatteryIndicatorColor.getKey(), "0")));

        mBatteryIndicatorColor.setOnPreferenceChangeListener((preference, o) -> {
            showBatteryIndicatorColor(Integer.parseInt((String) o));
            return true;
        });


        mBatteryIndicatorTest = findPreference("prefs_key_system_ui_status_bar_battery_indicator_test");
        mBatteryIndicatorTest.setOnPreferenceClickListener(preference -> {
            requireActivity().sendBroadcast(new Intent("moralnorm.module.BatteryIndicatorTest"));
            return true;
        });
    }

    private void showBatteryIndicatorColor(int vale) {
        boolean showBatteryIndicatorColor = vale != 2;
        mBatteryIndicatorFullPower.setVisible(showBatteryIndicatorColor);
        mBatteryIndicatorLowPower.setVisible(showBatteryIndicatorColor);
        mBatteryIndicatorPowerSaving.setVisible(showBatteryIndicatorColor);
        mBatteryIndicatorPowerCharging.setVisible(showBatteryIndicatorColor);
    }
}
