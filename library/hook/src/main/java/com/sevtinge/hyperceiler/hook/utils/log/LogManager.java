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

import static com.sevtinge.hyperceiler.hook.utils.api.ProjectApi.isCanary;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getSerial;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mSharedPreferences;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LogManager {

    public static boolean IS_LOGGER_ALIVE;
    public static final int logLevel = getLogLevel();
    public static String LOGGER_CHECKER_ERR_CODE;
    private static final String LOG_CONFIG_PATH = "/files/log_config";

    public static void init() {
        IS_LOGGER_ALIVE = isLoggerAlive();
    }

    public static void setLogLevel() {
        int logLevel = Integer.parseInt(mSharedPreferences.getString("prefs_key_log_level", "3"));
        int effectiveLogLevel = isCanary() ? (logLevel != 3 && logLevel != 4 ? 3 : logLevel) : logLevel;
        writeLogLevelToFile(null, effectiveLogLevel);
    }

    public static void setLogLevel(int level, String basePath) {
        int effectiveLogLevel = isCanary() ? (level != 3 && level != 4 ? 3 : level) : level;
        writeLogLevelToFile(basePath, effectiveLogLevel);
    }

    private static void writeLogLevelToFile(String basePath, int level) {
        try {
            String configPath = (basePath != null ? basePath : "") + LOG_CONFIG_PATH;
            File configFile = new File(configPath);
            File configDir = configFile.getParentFile();

            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs();
            }

            // 使用 FileLock 进行多进程安全读写
            try (FileChannel channel = FileChannel.open(configFile.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                FileLock lock = channel.lock();
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(String.valueOf(level));
                    writer.flush();
                } finally {
                    lock.release();
                }
            }
        } catch (Exception e) {
            Log.e("LogManager", "Failed to write log level to file: ", e);
        }
    }

    public static int readLogLevelFromFile(String basePath) {
        try {
            String configPath = (basePath != null ? basePath : "") + LOG_CONFIG_PATH;
            File configFile = new File(configPath);

            if (configFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line = reader.readLine();
                    if (line != null) {
                        try {
                            int level = Integer.parseInt(line.trim());
                            if (level >= 0 && level <= 4) {
                                return level;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LogManager", "Failed to read log level from file: ", e);
        }

        // Default fallback
        int level = mPrefsMap.getStringAsInt("log_level", 3);
        return isCanary() ? (level != 3 && level != 4 ? 3 : level) : level;
    }

    public static int getLogLevel() {
        int level = mPrefsMap.getStringAsInt("log_level", 3);
        return isCanary() ? (level != 3 && level != 4 ? 3 : level) : level;
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
            boolean lsposedLogDirExists = !rootExecCmd("ls -d /data/adb/lspd/log/ 2>/dev/null").isEmpty();

            if (lsposedLogDirExists) {
                String latestLogFile = rootExecCmd("ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1").trim();

                if (!latestLogFile.isEmpty() && !latestLogFile.contains("No such file")) {
                    String grepOutput = rootExecCmd("grep -i -q 'HyperCeiler' " + latestLogFile + " && echo 'FOUND' || echo 'EMPTY'");
                    if (grepOutput.trim().equals("EMPTY")) {
                        LOGGER_CHECKER_ERR_CODE = "EMPTY_XPOSED_LOG_FILE";
                        return false;
                    }
                } else {
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "brief", "-s", tag + ":D"});

                try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains(message)) {
                            LOGGER_CHECKER_ERR_CODE = "SUCCESS";
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER_CHECKER_ERR_CODE = String.valueOf(e);
            } finally {
                if (process != null) process.destroy();
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

    public static String fixLSPosedLogService() {
        try {
            rootExecCmd("resetprop -n persist.log.tag.LSPosed V");
            rootExecCmd("resetprop -n persist.log.tag.LSPosed-Bridge V");
            return "SUCCESS";
        } catch (Exception e) {
            return e.toString();
        }
    }
}
