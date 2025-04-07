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
package com.sevtinge.hyperceiler.hook.utils.log;


import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getSerial;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mSharedPreferences;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.hook.BuildConfig;
import com.sevtinge.hyperceiler.hook.utils.PropUtils;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LogManager {

    public static boolean IS_LOGGER_ALIVE;
    public static final int logLevel = getLogLevel();
    public static String LOGGER_CHECKER_ERR_CODE;
    private static String PROP_HYPERCEILER_LOG_LEVEL = "persist.hyperceiler.log.level";

    public static void init() {
        IS_LOGGER_ALIVE = isLoggerAlive();
    }

    public static void setLogLevel() {
        int logLevel = Integer.parseInt(mSharedPreferences.getString("prefs_key_log_level", "3"));
        int effectiveLogLevel = ProjectApi.isCanary() ? (logLevel != 3 && logLevel != 4 ? 3 : logLevel) : logLevel;
        PropUtils.setProp(PROP_HYPERCEILER_LOG_LEVEL, effectiveLogLevel);
    }

    public static int getLogLevel() {
        int level = mPrefsMap.getStringAsInt("log_level", 3);
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
        try {
            String modulesOutput = rootExecCmd("ls /data/adb/modules/");
            String[] moduleLines = modulesOutput.split("\n");
            boolean lsposedFound = false;
            for (String line : moduleLines) {
                if (line.toLowerCase().contains("lsposed")) {
                    lsposedFound = true;
                    break;
                }
            }
            if (lsposedFound) {
                String output = rootExecCmd("ls /data/adb/lspd/log/");
                String[] lines = output.split("\n");
                List<String> logFiles = new ArrayList<>();
                for (String line : lines) {
                    if (line.startsWith("modules_") && line.endsWith(".log")) {
                        logFiles.add(line);
                    }
                }

                if (logFiles.size() == 1) {
                    String fileName = logFiles.get(0);
                    String filePath = "/data/adb/lspd/log/" + fileName;
                    String grepOutput = rootExecCmd("grep -q 'HyperCeiler' " + filePath + " && echo 'FOUND' || echo 'EMPTY'");
                    if (grepOutput.trim().equals("EMPTY")) {
                        grepOutput = rootExecCmd("grep -q 'hyperceiler' " + filePath + " && echo 'FOUND' || echo 'EMPTY'");
                        if (grepOutput.trim().equals("EMPTY")) {
                            LOGGER_CHECKER_ERR_CODE = "EMPTY_XPOSED_LOG_FILE";
                            return false;
                        }
                    }
                } else if (logFiles.isEmpty()) {
                    LOGGER_CHECKER_ERR_CODE = "NO_XPOSED_LOG_FILE";
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER_CHECKER_ERR_CODE = String.valueOf(e);
        }

        String tag = "HyperCeilerLogManager";
        String message = "LOGGER_ALIVE_SYMBOL_" + getSerial();
        int timeout = 5;
        Log.d(tag, message);

        ExecutorService executor = Executors.newCachedThreadPool();
        Future<Boolean> future = executor.submit(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(Runtime.getRuntime().exec("logcat -d " + tag + ":D *:S").getInputStream()))) {

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

    public static String fixLsposedLogService() {
        try {
            rootExecCmd("resetprop -n persist.log.tag.LSPosed V");
            rootExecCmd("resetprop -n persist.log.tag.LSPosed-Bridge V");
            return "SUCCESS";
        } catch (Exception e) {
            return e.toString();
        }
    }
}
