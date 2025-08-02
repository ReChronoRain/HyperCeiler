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

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isFullSupport;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.R;

public class SupportVersionFragment extends SettingsPreferenceFragment {

    private static final String F_SUPPORT_HYPER_OS_VERSION = "1.0 / 2.0 / 2.0.100 / 2.0.200";
    private static final String F_SUPPORT_ANDROID_VERSION = "14(U, 34) / 15(V, 35)";
    private static final String N_SUPPORT_HYPER_OS_VERSION = "1.1 / 2.0.230";
    private static final String N_SUPPORT_ANDROID_VERSION = "16(B, 36)"; // 暂定名

    Preference helpSupportVersion;
    LayoutPreference supportFullVersion;
    LayoutPreference supportNotVersion;
    private String cachedSummary;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_help_support_version;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        helpSupportVersion = findPreference("prefs_key_textview_help_support_version");
        supportFullVersion = findPreference("prefs_key_textview_full_support_version");
        supportNotVersion = findPreference("prefs_key_textview_not_support_version");

        boolean fullSupport = isFullSupport();
        if (supportFullVersion != null) supportFullVersion.setVisible(fullSupport);
        if (supportNotVersion != null) supportNotVersion.setVisible(!fullSupport);

        if (helpSupportVersion != null) {
            if (cachedSummary == null) {
                cachedSummary = buildSummary();
            }
            helpSupportVersion.setSummary(cachedSummary);
        }
    }

    private String buildSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.help_support_version_desc_1))
          .append("\n - HyperOS ").append(F_SUPPORT_HYPER_OS_VERSION)
          .append("\n - Android ").append(F_SUPPORT_ANDROID_VERSION);

        boolean hasNSupport = !N_SUPPORT_HYPER_OS_VERSION.isEmpty() || !N_SUPPORT_ANDROID_VERSION.isEmpty();
        if (hasNSupport) {
            sb.append("\n\n").append(getString(R.string.help_support_version_desc_2));
            if (!N_SUPPORT_HYPER_OS_VERSION.isEmpty()) {
                sb.append("\n\n - HyperOS ").append(N_SUPPORT_HYPER_OS_VERSION);
            }
            if (!N_SUPPORT_ANDROID_VERSION.isEmpty()) {
                sb.append("\n - Android ").append(N_SUPPORT_ANDROID_VERSION);
            }
        }
        return sb.toString();
    }
}
