package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

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

        SwitchPreference mFixAndroidRS;

        @Override
        public void initPrefs() {
            mFixAndroidRS = findPreference("prefs_key_home_other_fix_android_r_s");
            mFixAndroidRS.setVisible(!SdkHelper.isAndroidTiramisu());
        }
    }
}
