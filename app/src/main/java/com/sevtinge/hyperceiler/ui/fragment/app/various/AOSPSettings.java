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
package com.sevtinge.hyperceiler.ui.fragment.app.various;

import android.content.Intent;
import androidx.preference.Preference;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

public class AOSPSettings extends SettingsPreferenceFragment {
    private Preference mBattery;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.various_aosp;
    }

    @Override
    public void initPrefs() {
        mBattery = findPreference("prefs_key_various_open_aosp_battery");

        if (mBattery != null) {
            mBattery.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent();
                String className = isMoreAndroidVersion(35) ?
                    "com.android.settings.Settings$AppBatteryUsageActivity" :
                    "com.android.settings.Settings$HighPowerApplicationsActivity";
                intent.setClassName("com.android.settings", className);
                startActivity(intent);
                return true;
            });
        }
    }
}