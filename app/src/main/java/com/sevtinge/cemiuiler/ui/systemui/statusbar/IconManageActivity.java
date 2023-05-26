package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;

public class IconManageActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new IconManageFragment();
    }

    public static class IconManageFragment extends SubFragment {
    
        Preference UseNewHD;
        DropDownPreference IconNewHD;
        DropDownPreference mAlarmClockIcon;
        SeekBarPreference mAlarmClockIconN;
        SeekBarPreference mNotificationIconMaximum;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_icon_manage;
        }

        @Override
        public void initPrefs() {
            mAlarmClockIcon = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock");
            mAlarmClockIconN = findPreference("prefs_key_system_ui_status_bar_icon_alarm_clock_n");
            mNotificationIconMaximum = findPreference("prefs_key_system_ui_status_bar_notification_icon_maximum");
            
            UseNewHD = findPreference("prefs_key_system_ui_status_bar_use_new_hd");
            IconNewHD = findPreference("prefs_key_system_ui_status_bar_icon_new_hd");
            UseNewHD.setVisible(SdkHelper.isAndroidTiramisu());
            IconNewHD.setVisible(SdkHelper.isAndroidTiramisu());

            mAlarmClockIconN.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_status_bar_icon_alarm_clock", "0")) == 3);

            mAlarmClockIcon.setOnPreferenceChangeListener((preference, o) -> {
                mAlarmClockIconN.setVisible(Integer.parseInt((String) o) == 3);
                return true;
            });

            mNotificationIconMaximum.setOnPreferenceChangeListener((preference, o) -> {
                if ((int)o == 16) {
                    mNotificationIconMaximum.setValue(R.string.unlimited);
                }
                return true;
            });
        }
    }
}
