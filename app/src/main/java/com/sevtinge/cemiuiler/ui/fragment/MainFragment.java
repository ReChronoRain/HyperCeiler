package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class MainFragment extends SettingsPreferenceFragment {

    Preference mPowerSetting;
    Preference mMTB;

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        mPowerSetting = findPreference("prefs_key_powerkeeper");
        mMTB = findPreference("prefs_key_mtb");

        mPowerSetting.setVisible(!isAndroidR());
        mMTB.setVisible(!isAndroidR());
    }
}
