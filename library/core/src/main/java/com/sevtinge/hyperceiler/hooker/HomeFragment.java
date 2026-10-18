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
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;

import android.content.Context;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;

public class HomeFragment extends DashboardFragment {

    LayoutPreference mHeader;
    LayoutPreference mHeaderHomeIsRust;
    LayoutPreference mHeaderHomeIsRustNoSupport;
    private static final String PREF_KEY_VERSION_CODE = "prefs_key_framework_check_version_code";
    private static final String PREF_KEY_API_VERSION = "prefs_key_framework_check_api_version";
    private SharedPreferences mFrameworkPrefs;
    private boolean mHomeIsRuntime;
    private final SharedPreferences.OnSharedPreferenceChangeListener mFrameworkListener =
        (prefs, key) -> {
            if (key == null || PREF_KEY_VERSION_CODE.equals(key) || PREF_KEY_API_VERSION.equals(key)) {
                Activity activity = getActivity();
                if (activity == null) return;
                activity.runOnUiThread(() -> {
                    if (isResumed()) updateRuntimeSupportWarning();
                });
            }
        };

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
        mHeaderHomeIsRust = findPreference("prefs_key_home_is_rust");
        mHeaderHomeIsRustNoSupport = findPreference("prefs_key_home_is_rust_no_support");

        boolean check = CheckModifyUtils.INSTANCE.getCheckResult(getContext(), "com.miui.home");
        boolean isDebugMode = getSharedPreferences().getBoolean("prefs_key_development_debug_mode", false);
        mHomeIsRuntime = isHyperOsPackage(getContext(), "com.miui.home");

        mHeader.setVisible(check && !isDebugMode);
        if (mHeaderHomeIsRust != null) mHeaderHomeIsRust.setVisible(mHomeIsRuntime);
        updateRuntimeSupportWarning();
        if (findPreference("prefs_key_home_os4_unadapted_tip") != null) {
            findPreference("prefs_key_home_os4_unadapted_tip").setVisible(isHyperOSVersion(4f));
        }
    }

    // Preserve the existing numeric LSPosed baseline. A version name suffix
    // such as "-it" is branding, not a runtime-hook capability requirement.
    // Missing service metadata is pending, not evidence of incompatibility.
    static boolean shouldWarnRuntimeUnsupported(boolean runtime, int api, long code) {
        return runtime && api > 0 && code > 0 && (api < 102 || code < 7846);
    }

    private void updateRuntimeSupportWarning() {
        if (mHeaderHomeIsRustNoSupport == null) return;
        SharedPreferences prefs = getSharedPreferences();
        int api = prefs == null ? -1 : prefs.getInt(PREF_KEY_API_VERSION, -1);
        long code = prefs == null ? -1 : prefs.getLong(PREF_KEY_VERSION_CODE, -1);
        mHeaderHomeIsRustNoSupport.setVisible(shouldWarnRuntimeUnsupported(mHomeIsRuntime, api, code));
    }

    @Override
    public void onResume() {
        super.onResume();
        mFrameworkPrefs = getSharedPreferences();
        if (mFrameworkPrefs != null) mFrameworkPrefs.registerOnSharedPreferenceChangeListener(mFrameworkListener);
        updateRuntimeSupportWarning();
    }

    @Override
    public void onPause() {
        if (mFrameworkPrefs != null) {
            mFrameworkPrefs.unregisterOnSharedPreferenceChangeListener(mFrameworkListener);
            mFrameworkPrefs = null;
        }
        super.onPause();
    }

}
