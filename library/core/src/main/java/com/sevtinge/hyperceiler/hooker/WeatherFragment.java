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
package com.sevtinge.hyperceiler.hooker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;

public class WeatherFragment extends DashboardFragment {

    String packageName = "com.miui.weather2";

    LayoutPreference mHeaderHomeIsRust;
    LayoutPreference mHeaderHomeIsRustNoSupport;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.weather;
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
        mHeaderHomeIsRust = findPreference("prefs_key_home_is_rust");
        mHeaderHomeIsRustNoSupport = findPreference("prefs_key_home_is_rust_no_support");

        final String PREF_KEY_VERSION = "prefs_key_framework_check_version";
        final String PREF_KEY_VERSION_CODE = "prefs_key_framework_check_version_code";
        final String PREF_KEY_API_VERSION = "prefs_key_framework_check_api_version";

        boolean isHyperOsPackage = isHyperOsPackage(getContext(), packageName);

        String XposedVersion = getSharedPreferences().getString(PREF_KEY_VERSION, "Unknown");
        long XposedVersionCode = getSharedPreferences().getLong(PREF_KEY_VERSION_CODE, 0);
        int XposedApiVersion = getSharedPreferences().getInt(PREF_KEY_API_VERSION, 0);

        boolean isRustNoSupport = !(XposedApiVersion >= 102
            && XposedVersionCode >= 7846
            && (XposedVersionCode >= 7854 || XposedVersion.contains("it")));

        mHeaderHomeIsRust.setVisible(isHyperOsPackage);
        mHeaderHomeIsRustNoSupport.setVisible(isRustNoSupport);
    }

}
