package com.sevtinge.cemiuiler.ui.fragment.framework;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidT;

import android.content.Intent;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.SubPickerActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends SettingsPreferenceFragment {

    Preference mCleanShareApps;
    Preference mCleanOpenApps;
    SwitchPreference mAppLinkVerify;
    SwitchPreference mUseOriginalAnim;
    SwitchPreference mDisableWaterMark;

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
        mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
        mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
        mAppLinkVerify.setVisible(!isAndroidR());
        mAppLinkVerify.setOnPreferenceChangeListener((preference, o) -> true);
        mUseOriginalAnim = findPreference("prefs_key_system_framework_other_use_original_animation");
        mUseOriginalAnim.setVisible(!isAndroidT());

        mCleanShareApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mCleanOpenApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mDisableWaterMark = findPreference("prefs_key_system_framework_disable_private_watermark");
        if (!getSharedPreferences().getBoolean("prefs_key_various_enable_super_function", false)) {
            mDisableWaterMark.setVisible(false);
        }
    }


}
