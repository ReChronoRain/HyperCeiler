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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.utils.XposedActivateHelper.isModuleActive;

import android.content.Context;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

public class LogServiceUtils {

    public static void init(Context context) {
        shouldShowLogServiceWarnDialog(context);
    }

    private static void shouldShowLogServiceWarnDialog(Context context) {
        if (showLogServiceWarn()) {
            DialogHelper.showLogServiceWarnDialog(context);
        }
    }

    private static boolean showLogServiceWarn() {
        return !IS_LOGGER_ALIVE && isModuleActive && BuildConfig.BUILD_TYPE != "release" &&
            !PrefsUtils.mSharedPreferences.getBoolean("prefs_key_development_close_log_alert_dialog", false);
    }
}
