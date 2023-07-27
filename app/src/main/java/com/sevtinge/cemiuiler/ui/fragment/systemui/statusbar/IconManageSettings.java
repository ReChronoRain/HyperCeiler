package com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidT;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class IconManageSettings extends SettingsPreferenceFragment {

    Preference UseNewHD;
    DropDownPreference IconNewHD;
    DropDownPreference mAlarmClockIcon;
    SeekBarPreferenceEx mAlarmClockIconN;
    SeekBarPreferenceEx mNotificationIconMaximum;
    SwitchPreference mBatteryNumber;
    SwitchPreference mBatteryPercentage;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_icon_manage;
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
        mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
        mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");
        mNotificationIconMaximum = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mBatteryNumber = findPreference("prefs_key_system_ui_status_bar_battery_percent");
        mBatteryPercentage = findPreference("prefs_key_system_ui_status_bar_battery_percent_mark");

        UseNewHD = findPreference("prefs_key_system_ui_status_bar_use_new_hd");
        IconNewHD = findPreference("prefs_key_system_ui_status_bar_icon_new_hd");
        UseNewHD.setVisible(isAndroidT());
        IconNewHD.setVisible(isAndroidT());

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
