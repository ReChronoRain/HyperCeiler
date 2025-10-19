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
package com.sevtinge.hyperceiler.hooker.framework;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.core.R;

public class CorePatchSettings extends DashboardFragment {

    SwitchPreference mDownGr;
    SwitchPreference mDisableCreak;
    SwitchPreference mDisableIntegrity;
    SwitchPreference mSharedUser;
    SwitchPreference mDigestCreak;
    SwitchPreference mExactSignatureCheck;
    SwitchPreference mUsePreSignature;

    SwitchPreference mLossFingerprint;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.framework_core_patch;
    }

    @Override
    public void initPrefs() {
        boolean mCreak = getSharedPreferences().getBoolean("prefs_key_system_framework_core_patch_auth_creak", false);
        boolean mEnable = getSharedPreferences().getBoolean("prefs_key_system_framework_core_patch_enable", false);
        mDownGr = findPreference("prefs_key_system_framework_core_patch_downgr");
        mDisableCreak = findPreference("prefs_key_system_framework_core_patch_auth_creak");
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mSharedUser = findPreference("prefs_key_system_framework_core_patch_shared_user");
        mDigestCreak = findPreference("prefs_key_system_framework_core_patch_digest_creak");
        mExactSignatureCheck = findPreference("prefs_key_system_framework_core_patch_exact_signature_check");
        mUsePreSignature = findPreference("prefs_key_system_framework_core_patch_use_pre_signature");

        mLossFingerprint = findPreference("prefs_key_system_framework_core_patch_unloss_fingerprint");

        mDownGr.setVisible(mEnable);
        mDisableCreak.setVisible(mEnable);
        mDisableIntegrity.setVisible(mEnable && !mCreak);
        mSharedUser.setVisible(mEnable);
        mDigestCreak.setVisible(mEnable);
        mExactSignatureCheck.setVisible(mEnable);
        mUsePreSignature.setVisible(mEnable);

        setHide(mLossFingerprint, isMoreAndroidVersion(36));

        findPreference("prefs_key_system_framework_core_patch_enable").setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                mDownGr.setVisible(true);
                mDisableCreak.setVisible(true);
                mDisableIntegrity.setVisible(!mCreak);
                mSharedUser.setVisible(true);
                mDigestCreak.setVisible(true);
                mExactSignatureCheck.setVisible(true);
                mUsePreSignature.setVisible(true);
            } else {
                mDownGr.setVisible(false);
                mDisableCreak.setVisible(false);
                mDisableIntegrity.setVisible(false);
                mSharedUser.setVisible(false);
                mDigestCreak.setVisible(false);
                mExactSignatureCheck.setVisible(false);
                mUsePreSignature.setVisible(false);
            }
            return true;
        });

        mDisableCreak.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                mDisableIntegrity.setChecked(false);
                mDisableIntegrity.setVisible(false);
            } else {
                mDisableIntegrity.setVisible(true);
            }
            return true;
        });
    }
}
