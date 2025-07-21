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
package com.sevtinge.hyperceiler.ui.page.settings.helper;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isFullSupport;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.R;

public class SupportVersionFragment extends SettingsPreferenceFragment {

    private static final String mFSupportHyperOsVersion = "1.0 / 2.0 / 2.0.100 / 2.0.200";
    private static final String mFSupportAndroidVersion = "14(U, 34) / 15(V, 35)";
    private static final String mNSupportHyperOsVersion = "1.1 / 2.0.230";
    private static final String mNSupportAndroidVersion = "16(B, 36)"; // 暂定名

    Preference mHelpSupportVersion;
    LayoutPreference mSupportFullVersion;
    LayoutPreference mSupportNotVersion;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_help_support_version;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        mHelpSupportVersion = findPreference("prefs_key_textview_help_support_version");
        mSupportFullVersion = findPreference("prefs_key_textview_full_support_version");
        mSupportNotVersion = findPreference("prefs_key_textview_not_support_version");

        if (isFullSupport()) {
            mSupportFullVersion.setVisible(true);
            mSupportNotVersion.setVisible(false);
        } else {
            mSupportFullVersion.setVisible(false);
            mSupportNotVersion.setVisible(true);
        }

        if (mHelpSupportVersion != null) {
            mHelpSupportVersion.setSummary(stringBuilder());
        }
    }

    private String stringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getString(R.string.help_support_version_desc_1))
                .append("\n - HyperOS ").append(mFSupportHyperOsVersion)
                .append("\n - Android ").append(mFSupportAndroidVersion);

        if (!mNSupportHyperOsVersion.isEmpty() || !mNSupportAndroidVersion.isEmpty()) {
            stringBuilder.append("\n\n").append(getString(R.string.help_support_version_desc_2));
            if (!mNSupportHyperOsVersion.isEmpty()) {
                stringBuilder.append("\n\n - HyperOS ").append(mNSupportHyperOsVersion);
            }
            if (!mNSupportAndroidVersion.isEmpty()) {
                stringBuilder.append("\n - Android ").append(mNSupportAndroidVersion);
            }
        }

        return stringBuilder.toString();
    }
}
