package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class HomeDrawerActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.home.HomeDrawerActivity.HomeDrawerFragment();
    }

    public static class HomeDrawerFragment extends SubFragment {

        SwitchPreference mAllAppsContainerViewBlur;

        @Override
        public int getContentResId() {
            return R.xml.home_drawer;
        }

        @Override
        public void initPrefs() {
            mAllAppsContainerViewBlur = findPreference("prefs_key_home_drawer_blur");
            mAllAppsContainerViewBlur.setVisible(!SdkHelper.isAndroidR());

            mAllAppsContainerViewBlur.setOnPreferenceChangeListener((preference, o) -> true);

        }
    }
}
