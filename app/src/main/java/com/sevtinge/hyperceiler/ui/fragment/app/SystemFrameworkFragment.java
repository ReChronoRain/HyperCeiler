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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.devicesdk.TelephonyManager;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

public class SystemFrameworkFragment extends SettingsPreferenceFragment {
    SwitchPreference mDisableCreak;
    SwitchPreference mShareUser;
    SwitchPreference mDisableIntegrity;
    SwitchPreference mDisableLowApiCheck;
    SwitchPreference mDisablePersistent;
    SwitchPreference mIsolationViolation;
    Preference mNetwork;

    @Override
    public int getContentResId() {
        return R.xml.framework;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }

    @Override
    public void initPrefs() {
        boolean mCreak = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_framework_core_patch_auth_creak", false);
        mDisableCreak = findPreference("prefs_key_system_framework_core_patch_auth_creak");
        mShareUser = findPreference("prefs_key_system_framework_core_patch_shared_user");
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mDisableLowApiCheck = findPreference("prefs_key_system_framework_disable_low_api_check");
        mDisablePersistent = findPreference("prefs_key_system_framework_disable_persistent");
        mNetwork = findPreference("prefs_key_system_framework_network");
        mIsolationViolation = findPreference("prefs_key_system_framework_core_patch_bypass_isolation_violation");

        mDisableIntegrity.setVisible(isMoreAndroidVersion(33) && !mCreak);
        mShareUser.setVisible(isMoreAndroidVersion(33)); // 暂时仅开放给 Android 13 及以上使用
        mNetwork.setVisible(TelephonyManager.getDefault().isFiveGCapable());
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
