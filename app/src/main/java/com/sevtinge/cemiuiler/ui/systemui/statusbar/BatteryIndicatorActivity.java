package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.util.Objects;

import moralnorm.preference.ColorPickerPreference;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;

public class BatteryIndicatorActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new BatteryIndicatorFragment();
    }

    public static class BatteryIndicatorFragment extends SubFragment {

        DropDownPreference mBatteryIndicatorColor;
        ColorPickerPreference mBatteryIndicatorFullPower;
        ColorPickerPreference mBatteryIndicatorLowPower;
        ColorPickerPreference mBatteryIndicatorPowerSaving;
        ColorPickerPreference mBatteryIndicatorPowerCharging;
        Preference mBatteryIndicatorTest;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_battery_indicator;
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
}
