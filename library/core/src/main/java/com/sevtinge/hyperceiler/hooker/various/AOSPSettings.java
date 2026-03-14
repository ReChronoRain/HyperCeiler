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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker.various;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class AOSPSettings extends DashboardFragment {
    Preference mBattery;
    Preference mDefaultApps;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.various_aosp;
    }

    @Override
    public void initPrefs() {
        mBattery = findPreference("prefs_key_various_open_aosp_battery");
        mDefaultApps = findPreference("prefs_key_various_open_aosp_default_apps");

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

        if (mDefaultApps != null) {
            mDefaultApps.setOnPreferenceClickListener(preference -> {
                if (launchIfAvailable(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) {
                    return true;
                }

                Intent legacyIntent = new Intent();
                legacyIntent.setComponent(new ComponentName(
                    "com.android.permissioncontroller",
                    "com.android.permissioncontroller.role.ui.DefaultAppListActivity"
                ));
                if (launchIfAvailable(legacyIntent)) {
                    return true;
                }

                Toast.makeText(requireContext(), R.string.unsupported_system_func, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    private boolean launchIfAvailable(Intent intent) {
        if (intent.resolveActivity(requireContext().getPackageManager()) == null) {
            return false;
        }
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ignored) {
            return false;
        }
    }
}
