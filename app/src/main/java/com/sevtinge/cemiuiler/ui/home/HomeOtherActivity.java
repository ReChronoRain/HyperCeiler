package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;
import moralnorm.os.SdkVersion;
import moralnorm.preference.SwitchPreference;

public class HomeOtherActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new HomeOtherFragment();
    }

    public static class HomeOtherFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.home_other;
        }

        SwitchPreference mDisableMonoChrome;

        @Override
        public void initPrefs() {
            mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");
            mDisableMonoChrome.setVisible(SdkVersion.isAndroidT);

            mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> {
                return true;
            });
        }
    }
}
