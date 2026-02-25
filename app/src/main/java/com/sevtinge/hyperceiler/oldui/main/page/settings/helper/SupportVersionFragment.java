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
package com.sevtinge.hyperceiler.oldui.main.page.settings.helper;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_FULL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_NOT;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_PARTIAL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSupportStatus;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getVersionListText;

import android.os.Build;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;

public class SupportVersionFragment extends SettingsPreferenceFragment {

    private Preference helpSupportVersion;
    private LayoutPreference supportFullVersion;
    private LayoutPreference supportPartialVersion;
    private LayoutPreference supportNotVersion;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_help_support_version;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);

        helpSupportVersion = findPreference("prefs_key_textview_help_support_version");
        supportFullVersion = findPreference("prefs_key_textview_full_support_version");
        supportPartialVersion = findPreference("prefs_key_textview_partial_support_version");
        supportNotVersion = findPreference("prefs_key_textview_not_support_version");

        int currentStatus = getSupportStatus();

        if (supportFullVersion != null) {
            supportFullVersion.setVisible(currentStatus == SUPPORT_FULL);
        }
        if (supportPartialVersion != null) {
            supportPartialVersion.setVisible(currentStatus == SUPPORT_PARTIAL);
        }
        if (supportNotVersion != null) {
            supportNotVersion.setVisible(currentStatus == SUPPORT_NOT);
        }

        if (helpSupportVersion != null) {
            helpSupportVersion.setSummary(buildSummary());
        }
    }

    private String buildSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append(getString(R.string.help_current_system_version))
            .append("\n")
            .append(getCurrentVersionText());


        String fullList = getVersionListText(SUPPORT_FULL);
        if (!fullList.isEmpty()) {
            sb.append("\n\n")
                .append(getString(R.string.help_full_support_desc))
                .append("\n")
                .append(fullList);
        }

        // 部分适配
        String partialList = getVersionListText(SUPPORT_PARTIAL);
        if (!partialList.isEmpty()) {
            sb.append("\n\n")
                .append(getString(R.string.help_partial_support_desc))
                .append("\n")
                .append(partialList);
        }

        // 未适配
        String notList = getVersionListText(SUPPORT_NOT);
        if (!notList.isEmpty()) {
            sb.append("\n\n")
                .append(getString(R.string.help_not_support_desc))
                .append("\n")
                .append(notList);
        }

        return sb.toString();
    }

    private String getCurrentVersionText() {
        String version = getSystemVersionIncremental().substring(2);
        int androidVersion = getAndroidVersion();
        return "Android " + androidVersion + " - HyperOS " + version;
    }

    private int getAndroidVersion() {
        return switch (Build.VERSION.SDK_INT) {
            case 37 -> 17;
            case 36 -> 16;
            case 35 -> 15;
            default -> Build.VERSION.SDK_INT;
        };
    }

}
