package com.sevtinge.hyperceiler.ui.fragment.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
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
    Preference mClipboardWhitelistApps;
    SwitchPreference mAppLinkVerify;
    SwitchPreference mUseOriginalAnim;
    SwitchPreference mVerifyDisable;
    SwitchPreference mDisableDeviceLog; // 关闭访问设备日志确认

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
        mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
        mClipboardWhitelistApps =findPreference("prefs_key_system_framework_clipboard_whitelist_apps");
        mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
        mVerifyDisable = findPreference("prefs_key_system_framework_disable_verify_can_ve_disabled");
        mUseOriginalAnim = findPreference("prefs_key_system_framework_other_use_original_animation");

        mDisableDeviceLog = findPreference("prefs_key_various_disable_access_device_logs");

        mAppLinkVerify.setVisible(!isAndroidVersion(30));
        mVerifyDisable.setVisible(isMoreHyperOSVersion(1f));
        mUseOriginalAnim.setVisible(!isAndroidVersion(33));
        mDisableDeviceLog.setVisible(isMoreAndroidVersion(33));

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

        mClipboardWhitelistApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });
    }


}
