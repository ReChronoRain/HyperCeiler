package com.sevtinge.hyperceiler.ui.fragment.app.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

public class CorePatchSettings extends DashboardFragment {

    SwitchPreference mDisableCreak;
    SwitchPreference mShareUser;
    SwitchPreference mDisableIntegrity;
    SwitchPreference mDisableLowApiCheck;
    SwitchPreference mDisablePersistent;
    SwitchPreference mIsolationViolation;
    SwitchPreference mAllowUpdateSystemApps;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework_core_patch;
    }

    @Override
    public void initPrefs() {
        boolean mCreak = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_framework_core_patch_auth_creak", false);
        mDisableCreak = findPreference("prefs_key_system_framework_core_patch_auth_creak");
        mShareUser = findPreference("prefs_key_system_framework_core_patch_shared_user");
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mDisableLowApiCheck = findPreference("prefs_key_system_framework_disable_low_api_check");
        mDisablePersistent = findPreference("prefs_key_system_framework_disable_persistent");
        mIsolationViolation = findPreference("prefs_key_system_framework_core_patch_bypass_isolation_violation");
        mAllowUpdateSystemApps = findPreference("prefs_key_system_framework_core_patch_allow_update_system_app");

        mDisableIntegrity.setVisible(isMoreAndroidVersion(33) && !mCreak);
        mShareUser.setVisible(isMoreAndroidVersion(33)); // 暂时仅开放给 Android 13 及以上使用

        mAllowUpdateSystemApps.setVisible(isMoreAndroidVersion(35));
        mDisableLowApiCheck.setVisible(isMoreAndroidVersion(34));
        mDisablePersistent.setVisible(isMoreAndroidVersion(34));
        mIsolationViolation.setVisible(isMoreHyperOSVersion(2f));

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
