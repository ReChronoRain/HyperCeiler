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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hooker;


import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;

public class HomeFragment extends DashboardFragment {

    LayoutPreference mHeader;

    @Override
    public int getPreferenceScreenResId() {
        if (isMoreHyperOSVersion(3f)) {
            return R.xml.home_new;
        }
        return R.xml.home;
    }

    public static boolean isHyperOsPackage(Context context, String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                .getApplicationInfo(
                    packageName,
                    PackageManager.GET_META_DATA
                );

            Bundle metaData = appInfo.metaData;

            return metaData != null
                && metaData.getBoolean("hyperos_package", false);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void initPrefs() {
        mHeader = findPreference("prefs_key_home_unsupported");

        boolean check = CheckModifyUtils.INSTANCE.getCheckResult(getContext(), "com.miui.home");
        boolean isDebugMode = getSharedPreferences().getBoolean("prefs_key_development_debug_mode", false);
        boolean isHyperOsPackage = isHyperOsPackage(getContext(), "com.miui.home");
        if (isHyperOsPackage) {
            getSharedPreferences().edit().putBoolean("prefs_key_home_is_rust", true).apply();
        }

        mHeader.setVisible(check && !isDebugMode);
    }

}
