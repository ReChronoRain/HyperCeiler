/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.ui.fragment.main.helper;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isFullSupport;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class SupportVersionFragment extends SettingsPreferenceFragment {

    private final String mFSupportMiuiVersion = "13.0(130) / 13.1(130) / 13.2(130) / 14.0(140)";
    private final String mFSupportHyperOsVersion = "1.0(816/818)";
    private final String mFSupportAndroidVersion = "13(T, 33) / 14(U, 34)";
    private final String mNSupportHyperOsVersion = "1.1(816) / 2.0(816)";
    private final String mNSupportAndroidVersion = "15(V, 35)";

    Preference mHelpSupportVersion;

    @Override
    public int getContentResId() {
        return R.xml.prefs_help_support_version;
    }

    @Override
    public void initPrefs() {
        setTitle(R.string.help);
        mHelpSupportVersion = findPreference("prefs_key_textview_help_support_version");
        if (mHelpSupportVersion != null) {
            mHelpSupportVersion.setSummary(stringBuilder());
        }
    }

    private String stringBuilder() {
        return getString(R.string.help_support_version_desc_1) +
                "\n\n - MIUI " + mFSupportMiuiVersion +
                "\n - HyperOS " + mFSupportHyperOsVersion +
                "\n - Android " + mFSupportAndroidVersion +
                "\n\n" + getString(R.string.help_support_version_desc_2) +
                "\n\n - HyperOS " + mNSupportHyperOsVersion +
                "\n - Android " + mNSupportAndroidVersion +
                "\n\n" + (isFullSupport() ? getString(R.string.help_support_version_desc_3) : getString(R.string.help_support_version_desc_4));
    }
}
