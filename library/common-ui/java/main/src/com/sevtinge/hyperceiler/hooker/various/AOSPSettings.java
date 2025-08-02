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
package com.sevtinge.hyperceiler.hooker.various;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.R;

public class AOSPSettings extends DashboardFragment {
    Preference mBattery;
    Preference mLanguage;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.various_aosp;
    }

    @Override
    public void initPrefs() {
        mBattery = findPreference("prefs_key_various_open_aosp_battery");
        mLanguage = findPreference("prefs_key_various_open_aosp_language");

        if (isMoreHyperOSVersion(2f)) {
            setFuncHint(mLanguage, 1);
        }

        if (mBattery != null) {
            mBattery.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.SubSettings"));
                intent.putExtra(":settings:show_fragment", "com.android.settings.applications.manageapplications.ManageApplications");
                Bundle bundle = new Bundle();
                bundle.putString("classname", "com.android.settings.Settings$HighPowerApplicationsActivity");
                intent.putExtra(":settings:show_fragment_args", bundle);
                startActivity(intent);
                return true;
            });
        }
    }
}
