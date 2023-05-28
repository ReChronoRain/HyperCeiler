package com.sevtinge.cemiuiler.ui.home;


import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;
import moralnorm.preference.SwitchPreference;

public class HomeTitleActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new HomeTitleFragment();
    }

    public static class HomeTitleFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.home_title;
        }

        SwitchPreference mDisableMonoChrome;
        SwitchPreference mDisableMonetColor;

        @Override
        public void initPrefs() {
            mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");
            mDisableMonoChrome.setVisible(SdkHelper.isAndroidTiramisu());
            mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> true);
            mDisableMonetColor = findPreference("prefs_key_home_other_icon_monet_color");
            mDisableMonetColor.setVisible(SdkHelper.isAndroidTiramisu());
            mDisableMonetColor.setOnPreferenceChangeListener((preference, o) -> true);
        }
    }

}
