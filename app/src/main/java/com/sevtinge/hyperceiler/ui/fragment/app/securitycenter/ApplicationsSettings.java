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
package com.sevtinge.hyperceiler.ui.fragment.app.securitycenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Build;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;

import androidx.preference.SwitchPreference;

public class ApplicationsSettings extends DashboardFragment {

    SwitchPreference mUnlockAppSandbox;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.security_center_app;
    }

    @Override
    public void initPrefs() {
        mUnlockAppSandbox = findPreference("prefs_key_secutity_center_unlock_app_sandbox");
        mUnlockAppSandbox.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE));
    }
}
