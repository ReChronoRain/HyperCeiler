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
package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import miui.telephony.TelephonyManager;
import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class SystemFrameworkFragment extends SettingsPreferenceFragment {
    SwitchPreference mDisableCreak;
    SwitchPreference mDisableIntegrity;
    SwitchPreference mDisableLowApiCheck;
    Preference mNetwork;

    @Override
    public int getContentResId() {
        return R.xml.framework;
    }

    @Override
    public void initPrefs() {
        boolean mCreak = PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_framework_core_patch_auth_creak", false);
        mDisableCreak = findPreference("prefs_key_system_framework_core_patch_auth_creak");
        mDisableIntegrity = findPreference("prefs_key_system_framework_core_patch_disable_integrity");
        mDisableLowApiCheck = findPreference("prefs_key_system_framework_disable_low_api_check");
        mNetwork = findPreference("prefs_key_system_framework_network");

        mDisableIntegrity.setVisible(isMoreAndroidVersion(33) && !mCreak);
        mNetwork.setVisible(TelephonyManager.getDefault().isFiveGCapable());
        mDisableLowApiCheck.setVisible(isMoreAndroidVersion(34));

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
