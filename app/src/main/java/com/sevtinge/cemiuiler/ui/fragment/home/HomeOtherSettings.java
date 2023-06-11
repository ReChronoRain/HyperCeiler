package com.sevtinge.cemiuiler.ui.fragment.home;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class HomeOtherSettings extends SettingsPreferenceFragment {

    SwitchPreference mFixAndroidRS;

    @Override
    public int getContentResId() {
        return R.xml.home_other;
    }

    @Override
    public void initPrefs() {
        mFixAndroidRS = findPreference("prefs_key_home_other_fix_android_r_s");
        mFixAndroidRS.setVisible(!SdkHelper.isAndroidTiramisu());
    }
}
