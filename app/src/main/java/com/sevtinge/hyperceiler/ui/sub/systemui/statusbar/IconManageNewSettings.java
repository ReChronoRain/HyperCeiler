package com.sevtinge.hyperceiler.ui.sub.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;
import fan.preference.SwitchPreference;

public class IconManageNewSettings extends DashboardFragment {

    DropDownPreference mAlarmClockIcon;
    SeekBarPreferenceCompat mAlarmClockIconN;
    SeekBarPreferenceCompat mNotificationIconMaximum;
    SeekBarPreferenceCompat mNotificationIconColumns;
    SwitchPreference mBatteryNumber;
    SwitchPreference mBatteryPercentage;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_icon_manage_new;
    }

    @Override
    public void initPrefs() {
        mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
        mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");
        mNotificationIconMaximum = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mBatteryNumber = findPreference("prefs_key_system_ui_status_bar_battery_percent");
        mBatteryPercentage = findPreference("prefs_key_system_ui_status_bar_battery_percent_mark");
        mNotificationIconColumns = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mNotificationIconColumns.setDefaultValue((isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34)) ? 1 : 3);

        mAlarmClockIconN.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_status_bar_icon_alarm_clock", "0")) == 3);

        mAlarmClockIcon.setOnPreferenceChangeListener((preference, o) -> {
            mAlarmClockIconN.setVisible(Integer.parseInt((String) o) == 3);
            return true;
        });

        mNotificationIconMaximum.setOnPreferenceChangeListener((preference, o) -> {
            if ((int) o == 16) {
                mNotificationIconMaximum.setValue(R.string.unlimited);
            }
            return true;
        });

        mBatteryNumber.setOnPreferenceChangeListener((preference, o) -> {
            if (!(boolean) o) {
                mBatteryPercentage.setChecked(false);
            }
            return true;
        });
    }
}
