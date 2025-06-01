/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.hooker.framework;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

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
