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
package com.sevtinge.hyperceiler.utils.log;

import android.content.Context;
import android.util.Log;

import com.fan.common.logviewer.LogEntry;
import com.fan.common.logviewer.LogManager;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XposedLogLoader {

    private static final String TAG = "XposedLogLoader";

    private static final Pattern TIME_PATTERN = Pattern.compile("\\[\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    public static void loadLogs(Context context, Runnable callback) {
        LogManager logManager = LogManager.getInstance(context);
        logManager.clearXposedLogs();

        new Thread(() -> {
            try {
                String logFileCmd = "ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1";
                String logFilePath = ShellUtils.rootExecCmd(logFileCmd).trim();

                if (logFilePath.isEmpty() || logFilePath.contains("No such file") || logFilePath.contains("ls:")) {
                    logManager.addXposedLog(new LogEntry("W", "Xposed",
                            "No Xposed log file found.", "XposedLogLoader", true));
                    if (callback != null) callback.run();
                    return;
                }

                // 使用 grep 在 shell 层筛选，比 cat + Java 过滤更快
                String safeLogFilePath = logFilePath.replace("'", "'\"'\"'");
                String grepCmd = "grep 'HyperCeiler' '" + safeLogFilePath + "'";
                String content = ShellUtils.rootExecCmd(grepCmd);
                if (content == null || content.isEmpty()) {
                    logManager.addXposedLog(new LogEntry("I", "Xposed",
                            "No HyperCeiler logs found.", "XposedLogLoader", true));
                    if (callback != null) callback.run();
                    return;
                }

                BufferedReader reader = new BufferedReader(new StringReader(content));
                String line;
                List<LogEntry> entries = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    LogEntry entry = parseXposedLogLine(line);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
                reader.close();

                if (!entries.isEmpty()) {
                    logManager.addXposedLogs(entries);
                    Log.i(TAG, "Loaded " + entries.size() + " Xposed log entries");
                } else {
                    logManager.addXposedLog(new LogEntry("I", "Xposed",
                            "No HyperCeiler logs found.", "XposedLogLoader", true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to load Xposed logs", e);
                logManager.addXposedLog(new LogEntry("E", "Xposed",
                        "Failed to load logs: " + e.getMessage(), "XposedLogLoader", true));
            }

            if (callback != null) callback.run();
        }).start();
    }

    public static void loadLogsSync(Context context) {
        LogManager logManager = LogManager.getInstance(context);
        logManager.clearXposedLogs();

        try {
            String logFileCmd = "ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -n 1";
            String logFilePath = ShellUtils.rootExecCmd(logFileCmd).trim();

            if (logFilePath.isEmpty() || logFilePath.contains("No such file") || logFilePath.contains("ls:")) {
                logManager.addXposedLog(new LogEntry("W", "Xposed",
                        "No Xposed log file found.", "XposedLogLoader", true));
                return;
            }

            String content = ShellUtils.rootExecCmd("grep 'HyperCeiler' " + logFilePath);
            if (content == null || content.isEmpty()) {
                logManager.addXposedLog(new LogEntry("I", "Xposed",
                        "No HyperCeiler logs found.", "XposedLogLoader", true));
                return;
            }

            BufferedReader reader = new BufferedReader(new StringReader(content));
            String line;
            List<LogEntry> entries = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                LogEntry entry = parseXposedLogLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            reader.close();

            if (!entries.isEmpty()) {
                logManager.addXposedLogs(entries);
            } else {
                logManager.addXposedLog(new LogEntry("I", "Xposed",
                        "No HyperCeiler logs found.", "XposedLogLoader", true));
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to load Xposed logs", e);
            logManager.addXposedLog(new LogEntry("E", "Xposed",
                    "Failed to load logs: " + e.getMessage(), "XposedLogLoader", true));
        }
    }

    private static LogEntry parseXposedLogLine(String line) {
        // 解析时间戳
        long timestamp = System.currentTimeMillis();
        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        if (timeMatcher.find()) {
            try {
                timestamp = TIME_FORMAT.parse(timeMatcher.group(1)).getTime();
            } catch (Exception ignored) {}
        }

        // 解析日志等级
        String level = "V";
        if (line.contains("[E]")) level = "E";
        else if (line.contains("[W]")) level = "W";
        else if (line.contains("[I]")) level = "I";
        else if (line.contains("[D]")) level = "D";

        // 提取消息部分
        String message = line;
        int tagIndex = line.indexOf("[HyperCeiler]");
        if (tagIndex != -1) {
            message = line.substring(tagIndex);
        }

        // 提取包名作为标签
        String tag = "Other";
        int levelEndIndex = -1;
        for (String lvl : new String[]{"[I]", "[D]", "[W]", "[E]", "[V]"}) {
            int idx = message.indexOf(lvl);
            if (idx != -1) {
                levelEndIndex = idx + lvl.length();
                break;
            }
        }

        if (levelEndIndex != -1 && levelEndIndex < message.length() && message.charAt(levelEndIndex) == '[') {
            int pkgEndIndex = message.indexOf("]", levelEndIndex + 1);
            if (pkgEndIndex != -1) {
                String candidate = message.substring(levelEndIndex + 1, pkgEndIndex);
                if (isValidPackageName(candidate)) {
                    tag = candidate;
                }
            }
        }

        return new LogEntry(timestamp, level, "Xposed", message, tag, true);
    }

    private static boolean isValidPackageName(String name) {
        if (name == null || name.isEmpty()) return false;
        if ("android".equals(name)) return true;
        if (!name.contains(".")) return false;

        String[] parts = name.split("\\.");
        for (String part : parts) {
            if (part.isEmpty() || !Character.isLetter(part.charAt(0))) return false;
            for (char c : part.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') return false;
            }
        }
        return true;
    }
}
