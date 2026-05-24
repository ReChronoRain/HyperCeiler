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
package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public final class PermissionUtils {

    public static final String PERMISSION_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS";
    public static final String PERMISSION_QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES";

    private PermissionUtils() {}

    public static boolean hasPermission(Context context, String permission) {
        if (context == null || permission == null || permission.isEmpty()) {
            return false;
        }
        try {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasInstalledAppsPermission(Context context) {
        return hasPermission(context, PERMISSION_GET_INSTALLED_APPS);
    }

    public static boolean canReadInstalledApps(Context context) {
        return hasInstalledAppsPermission(context)
            || hasPermission(context, PERMISSION_QUERY_ALL_PACKAGES);
    }

    public static boolean isPermissionGranted(String permission, String[] permissions, int[] grantResults) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        if (permissions == null || grantResults == null) {
            return false;
        }
        int size = Math.min(permissions.length, grantResults.length);
        for (int i = 0; i < size; i++) {
            if (permission.equals(permissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    public static boolean isInstalledAppsPermissionGranted(String[] permissions, int[] grantResults) {
        return isPermissionGranted(PERMISSION_GET_INSTALLED_APPS, permissions, grantResults);
    }
}
