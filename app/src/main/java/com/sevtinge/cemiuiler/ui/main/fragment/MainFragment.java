package com.sevtinge.cemiuiler.ui.main.fragment;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

import moralnorm.os.SdkVersion;
import moralnorm.preference.Preference;

public class MainFragment extends SubFragment {

    Preference mPowerSetting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        mPowerSetting = findPreference("prefs_key_powerkeeper");
        mPowerSetting.setVisible(SdkVersion.isAndroidT);
    }
}
