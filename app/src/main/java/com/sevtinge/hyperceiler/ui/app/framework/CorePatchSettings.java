package com.sevtinge.hyperceiler.ui.app.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

public class CorePatchSettings extends DashboardFragment {

    SwitchPreference mDisableCreak;
    SwitchPreference mDisableIntegrity;
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
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mIsolationViolation = findPreference("prefs_key_system_framework_core_patch_bypass_isolation_violation");
        mAllowUpdateSystemApps = findPreference("prefs_key_system_framework_core_patch_allow_update_system_app");

        mDisableIntegrity.setVisible(!mCreak);

        mAllowUpdateSystemApps.setVisible(isMoreAndroidVersion(35));
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
