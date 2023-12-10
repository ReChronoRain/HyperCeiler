package com.sevtinge.hyperceiler.ui.fragment.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends SettingsPreferenceFragment {

    Preference mCleanShareApps;
    Preference mCleanOpenApps;
    SwitchPreference mAppLinkVerify;
    SwitchPreference mUseOriginalAnim;
    SwitchPreference mVerifyDisable;

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
        mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
        mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
        mVerifyDisable = findPreference("prefs_key_system_framework_disable_verify_can_ve_disabled");
        mAppLinkVerify.setVisible(!isAndroidVersion(30));
        mVerifyDisable.setVisible(isMoreHyperOSVersion(1f));
        mAppLinkVerify.setOnPreferenceChangeListener((preference, o) -> true);
        mUseOriginalAnim = findPreference("prefs_key_system_framework_other_use_original_animation");
        mUseOriginalAnim.setVisible(!isAndroidVersion(33));

        mCleanShareApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mCleanOpenApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });
    }


}
