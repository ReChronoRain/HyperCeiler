package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;

public class ClockIndicatorActivity extends BaseSystemUIActivity {
    @Override
    public Fragment initFragment() {
        return new ClockIndicatorActivity.ClockIndicatorFragment();
    }

    public static class ClockIndicatorFragment extends SubFragment {

/*
        PreferenceCategory mDefault;
        PreferenceCategory mGeek;
*/

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_clock_indicator;
        }

        /*public void onPreferenceChange(Preference preference, Object newValue) {
            preference.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) newValue);
        }*/

        /*@Override
        public void initPrefs() {
            int getMode = mPrefsMap.getInt("system_ui_statusbar_clock_mode", 1);
            mDefault = findPreference("prefs_key_system_ui_statusbar_clock_default");
            mGeek = findPreference("prefs_key_system_ui_statusbar_clock_geek");

            switch (getMode) {
                case 1:
                    if (mDefault != null) {
                        mDefault.setVisible(true);
                    }
                    if (mGeek != null) {
                        mGeek.setVisible(false);
                    }
                    break;
                case 2:
                    if (mDefault != null) {
                        mDefault.setVisible(false);
                    }
                    if (mGeek != null) {
                        mGeek.setVisible(true);
                    }
                    break;
            }
        }*/
    }
}