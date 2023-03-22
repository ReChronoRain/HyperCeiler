package com.sevtinge.cemiuiler.ui.systemui.statusbar;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemui.base.BaseSystemUIActivity;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;

public class ClockIndicatorActivity extends BaseSystemUIActivity {

    @Override
    public Fragment initFragment() {
        return new ClockIndicatorActivity.ClockIndicatorFragment();
    }

    public static class ClockIndicatorFragment extends SubFragment implements Preference.OnPreferenceChangeListener {

        DropDownPreference mClockModePreference;
        PreferenceCategory mDefault;
        PreferenceCategory mGeek;

        @Override
        public int getContentResId() {
            return R.xml.system_ui_status_bar_clock_indicator;
        }

        @Override
        public void initPrefs() {
            int mClockMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_clock_mode", "1"));
            mClockModePreference = findPreference("prefs_key_system_ui_statusbar_clock_mode");
            mDefault = findPreference("prefs_key_system_ui_statusbar_clock_default");
            mGeek = findPreference("prefs_key_system_ui_statusbar_clock_geek");

            setClockMode(mClockMode);
            mClockModePreference.setOnPreferenceChangeListener(this);

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if (preference == mClockModePreference) {
                setClockMode(Integer.parseInt((String)o));
            }
            return true;
        }

        private void setClockMode(int mode) {
            mDefault.setVisible(mode != 1);
            mGeek.setVisible(mode != 2);
        }

    }
}