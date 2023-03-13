package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.os.SdkVersion;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.SeekBarPreference;

public class IconManageActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new IconManageFragment();
    }

    public static class IconManageFragment extends SubFragment {
    
        Preference UseNewHD;

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
            UseNewHD.setVisible(SdkVersion.isAndroidT);

            mAlarmClockIconN.setVisible(Integer.parseInt((String) PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_status_bar_icon_alarm_clock", "0")) == 3);

            mAlarmClockIcon.setOnPreferenceChangeListener((preference, o) -> {
                mAlarmClockIconN.setVisible(Integer.parseInt((String) o) == 3);
                return true;
            });

            mNotificationIconMaximum.setOnSeekBarChangeListener(new SeekBarPreference.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (seekBar.getProgress() == 16) {
                        mNotificationIconMaximum.setValue(R.string.unlimited);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }
}
