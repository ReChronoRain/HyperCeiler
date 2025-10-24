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
package com.sevtinge.hyperceiler.main.page.settings.helper;

import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mDisableOrHiddenApp;
import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mNoScoped;
import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mUninstallApp;

import android.util.Log;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;

public class CantSeeAppsFragment extends SettingsPreferenceFragment {

    Preference mHelpCantSeeApps;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_help_cant_see_apps;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        mHelpCantSeeApps = findPreference("prefs_key_textview_help_cant_see_apps");
        String summary;
        if (mHelpCantSeeApps != null) {
            summary = getString(R.string.help_cant_see_apps_desc_no_hidden);
            if (!mDisableOrHiddenApp.isEmpty()) summary = summary + "\n\n" + getString(R.string.help_cant_see_apps_disable) + String.join("\n", mDisableOrHiddenApp);
            if (!mUninstallApp.isEmpty()) summary = summary + "\n\n" + getString(R.string.help_cant_see_apps_uninstall) + String.join("\n", mUninstallApp);
            if (!mNoScoped.isEmpty()) summary = summary + "\n\n" + getString(R.string.help_cant_see_apps_scope) + String.join("\n", mNoScoped);
            Log.d("mHelpCantSeeApps", "initPrefs: "+summary);
            mHelpCantSeeApps.setSummary(summary);
        }
    }
}
