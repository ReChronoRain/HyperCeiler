package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import miui.telephony.TelephonyManager;
import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class SystemFrameworkFragment extends SettingsPreferenceFragment {
    SwitchPreference mDisableCreak;
    SwitchPreference mDisableIntegrity;
    Preference mNetwork;

    @Override
    public int getContentResId() {
        return R.xml.framework;
    }

    @Override
    public void initPrefs() {
        mDisableCreak = findPreference("prefs_key_system_framework_core_patch_auth_creak");
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mNetwork = findPreference("prefs_key_system_framework_network");

        mDisableIntegrity.setVisible(isMoreAndroidVersion(33));
        mNetwork.setVisible(TelephonyManager.getDefault().isFiveGCapable());

        mDisableCreak.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                mDisableIntegrity.setChecked(false);
                mDisableIntegrity.setVisible(false);
            } else {
                mDisableIntegrity.setVisible(isMoreAndroidVersion(33));
            }
            return true;
        });
    }
}
