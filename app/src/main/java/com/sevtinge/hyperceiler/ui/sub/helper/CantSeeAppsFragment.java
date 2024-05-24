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
package com.sevtinge.hyperceiler.ui.sub.helper;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.Preference;

public class CantSeeAppsFragment extends DashboardFragment {

    Preference mHelpCantSeeApps;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.prefs_help_cant_see_apps;
    }

    @Override
    public void initPrefs() {
        mHelpCantSeeApps = findPreference("prefs_key_textview_help_cant_see_apps");
        if (mHelpCantSeeApps != null) {
            if (!PreferenceHeader.mUninstallApp.isEmpty() && !PreferenceHeader.mDisableOrHiddenApp.isEmpty()) {
                mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + getString(R.string.help_cant_see_apps_uninstall) +
                        String.join("\n", PreferenceHeader.mUninstallApp) + "\n" + getString(R.string.help_cant_see_apps_disable) +
                        String.join("\n", PreferenceHeader.mDisableOrHiddenApp));
            } else if (!PreferenceHeader.mUninstallApp.isEmpty()) {
                mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + getString(R.string.help_cant_see_apps_uninstall) +
                        String.join("\n", PreferenceHeader.mUninstallApp));
            } else if (!PreferenceHeader.mDisableOrHiddenApp.isEmpty()) {
                mHelpCantSeeApps.setSummary(getString(R.string.help_cant_see_apps_desc) + getString(R.string.help_cant_see_apps_disable) +
                        String.join("\n", PreferenceHeader.mDisableOrHiddenApp));
            }
        }
    }
}
