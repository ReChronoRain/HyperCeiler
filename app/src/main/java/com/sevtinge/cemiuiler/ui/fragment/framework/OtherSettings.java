package com.sevtinge.cemiuiler.ui.fragment.framework;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.devicesdk.SdkHelper;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends SettingsPreferenceFragment {

    Preference mCleanShareApps;
    Preference mCleanOpenApps;
    SwitchPreference mAppLinkVerify;
    SwitchPreference mUseOriginalAnim;

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
        mAppLinkVerify.setVisible(!SdkHelper.isAndroidR());
        mAppLinkVerify.setOnPreferenceChangeListener((preference, o) -> true);
        mUseOriginalAnim = findPreference("prefs_key_system_framework_other_use_original_animation");
        mUseOriginalAnim.setVisible(!SdkHelper.isAndroidTiramisu());
    }
}
