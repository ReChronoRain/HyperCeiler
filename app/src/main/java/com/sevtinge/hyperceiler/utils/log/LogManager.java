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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.log;


import android.util.Log;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.module.base.BaseXposedInit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LogManager {
    public static final int logLevel = getLogLevel();

    public static boolean IS_LOGGER_ALIVE;
    public static String LOGGER_CHECKER_ERR_CODE;

    public static int getLogLevel() {
        int level = BaseXposedInit.mPrefsMap.getStringAsInt("log_level", 3);
        return BuildConfig.BUILD_TYPE.equals("canary") ? (level != 3 && level != 4 ? 3 : level) : level;
    }

    public static String logLevelDesc() {
        return switch (logLevel) {
            case 0 -> ("Disable");
            case 1 -> ("Error");
            case 2 -> ("Warn");
            case 3 -> ("Info");
            case 4 -> ("Debug");
            default -> ("Unknown");
        };
    }

    public static boolean isLoggerAlive() {
        String tag = "111";
        String message = "11111";
        int timeout = 5;
        Log.d(tag, message);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d " + tag + ":D *:S");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains(message)) {
                        LOGGER_CHECKER_ERR_CODE = "SUCCESS";
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER_CHECKER_ERR_CODE = String.valueOf(e);
            }
            LOGGER_CHECKER_ERR_CODE = "NO_SUCH_LOG";
            return false;
        });

        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER_CHECKER_ERR_CODE = "TIME_OUT";
            future.cancel(true);
        } catch (Exception e) {
            LOGGER_CHECKER_ERR_CODE = String.valueOf(e);
        } finally {
            executor.shutdownNow();
        }

        LOGGER_CHECKER_ERR_CODE = "WITHOUT_CODE";
        return false;
    }
}
