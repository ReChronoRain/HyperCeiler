package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class IconManageNewSettings extends SettingsPreferenceFragment {

    DropDownPreference mAlarmClockIcon;
    SeekBarPreferenceEx mAlarmClockIconN;
    SeekBarPreferenceEx mNotificationIconMaximum;
    SeekBarPreferenceEx mNotificationIconColumns;
    SwitchPreference mBatteryNumber;
    SwitchPreference mBatteryPercentage;
    SwitchPreference mPaw;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_icon_manage_new;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mPaw = findPreference("prefs_key_system_ui_status_bar_icon_paw");
        mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
        mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");
        mNotificationIconMaximum = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mBatteryNumber = findPreference("prefs_key_system_ui_status_bar_battery_percent");
        mBatteryPercentage = findPreference("prefs_key_system_ui_status_bar_battery_percent_mark");
        mNotificationIconColumns = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");

        mNotificationIconColumns.setDefaultValue((isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34)) ? 1 : 3);

        if(Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_status_bar_icon_show_mobile_network_type", "0")) == 3) {
            mPaw.setEnabled(false);
            mPaw.setChecked(false);
        }
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
