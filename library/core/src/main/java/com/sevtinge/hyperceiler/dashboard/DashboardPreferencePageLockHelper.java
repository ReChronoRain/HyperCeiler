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
package com.sevtinge.hyperceiler.dashboard;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;

final class DashboardPreferencePageLockHelper {
    private final DashboardFragment mHost;
    private final String mWarningBannerKey;

    private boolean mPageLocked = false;
    private LayoutPreference mDynamicWarningBanner;

    DashboardPreferencePageLockHelper(@NonNull DashboardFragment host, @NonNull String warningBannerKey) {
        mHost = host;
        mWarningBannerKey = warningBannerKey;
    }

    void lockWithVersionUnavailableWarning() {
        lock(null, R.string.app_version_unavailable_warning_banner);
    }

    void lock(@Nullable Preference warningPreference, @Nullable Integer warningTitleResId) {
        String warningKey = ensureWarningPreference(warningPreference, warningTitleResId);
        if (mPageLocked) return;

        PreferenceScreen screen = mHost.getPreferenceScreen();
        if (screen == null) return;

        disableGroup(screen, warningKey);
        mPageLocked = true;
    }

    private String ensureWarningPreference(@Nullable Preference warningPreference, @Nullable Integer warningTitleResId) {
        if (warningPreference != null) {
            warningPreference.setVisible(true);
            warningPreference.setEnabled(false);
            if (warningTitleResId != null) {
                warningPreference.setTitle(warningTitleResId);
            }
            return warningPreference.getKey();
        }

        PreferenceScreen screen = mHost.getPreferenceScreen();
        if (screen == null) return null;

        if (mDynamicWarningBanner == null) {
            mDynamicWarningBanner = new LayoutPreference(mHost.requireContext(), R.layout.headtip_warn);
            mDynamicWarningBanner.setKey(mWarningBannerKey);
            mDynamicWarningBanner.setOrder(Integer.MIN_VALUE);
            mDynamicWarningBanner.setEnabled(false);
        }

        mDynamicWarningBanner.setVisible(true);
        if (warningTitleResId != null) {
            mDynamicWarningBanner.setTitle(warningTitleResId);
        }

        if (mHost.findPreference(mDynamicWarningBanner.getKey()) == null) {
            screen.addPreference(mDynamicWarningBanner);
        }
        return mDynamicWarningBanner.getKey();
    }

    private void disableGroup(@NonNull PreferenceGroup group, @Nullable String warningKey) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference == null) continue;

            if (preference instanceof PreferenceGroup childGroup) {
                disableGroup(childGroup, warningKey);
            }

            String key = preference.getKey();
            boolean isWarningBanner = !TextUtils.isEmpty(warningKey) && TextUtils.equals(warningKey, key);
            if (isWarningBanner) continue;

            if (!TextUtils.isEmpty(key)) {
                mHost.cleanKey(key);
            }
            preference.setEnabled(false);
        }
    }
}
