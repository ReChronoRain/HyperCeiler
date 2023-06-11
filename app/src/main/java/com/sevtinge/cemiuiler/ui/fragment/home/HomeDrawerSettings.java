package com.sevtinge.cemiuiler.ui.fragment.home;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class HomeDrawerSettings extends SettingsPreferenceFragment {

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
