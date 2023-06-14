package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

public class MainFragment extends SettingsPreferenceFragment {

    Preference mPowerSetting;
    Preference mMTB;
    Preference mSecurityCenter;
    Preference mSecurityCenterPad;

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        mPowerSetting = findPreference("prefs_key_powerkeeper");
        mMTB = findPreference("prefs_key_mtb");
        mSecurityCenter = findPreference("prefs_key_security_center");
        mSecurityCenterPad = findPreference("prefs_key_security_center_pad");

        mPowerSetting.setVisible(!isAndroidR());
        mMTB.setVisible(!isAndroidR());

       mSecurityCenter.setVisible(!isPad());
       mSecurityCenterPad.setVisible(isPad());
    }
}
