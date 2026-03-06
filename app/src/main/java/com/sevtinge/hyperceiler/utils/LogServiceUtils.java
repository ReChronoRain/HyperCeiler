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
package com.sevtinge.hyperceiler.utils;


import static com.sevtinge.hyperceiler.Application.isModuleActivated;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.libhook.utils.log.LogManager;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

public class LogServiceUtils {
    private static final long HEALTH_CHECK_TIMEOUT_MS = 10_000;

    public static void init(Context context) {
        // 在后台线程等待健康检查完成，再回到主线程决定是否弹窗
        new Thread(() -> {
            LogManager.awaitHealthCheck(HEALTH_CHECK_TIMEOUT_MS);
            if (shouldShowLogServiceWarn()) {
                new Handler(Looper.getMainLooper()).post(() ->
                    DialogHelper.showLogServiceWarnDialog(context)
                );
            }
        }, "LogServiceCheck").start();
    }

    private static boolean shouldShowLogServiceWarn() {
        return !LogManager.IS_LOGGER_ALIVE && isModuleActivated && !isRelease() &&
            !PrefsUtils.mPrefsMap.getBoolean("prefs_key_development_close_log_alert_dialog", false);
    }
}
