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
package com.fan.common.logviewer;

import static com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.checkRootPermission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XposedLogLoader {

    private static final String TAG = "XposedLogLoader";
    private static volatile XposedLogLoader instance;

    // LSPosed 日志路径
    private static final String LSPD_LOG_DIR = "/data/adb/lspd/log";
    private static final String LSPD_LOG_OLD_DIR = "/data/adb/lspd/log.old";

    // 应用私有日志路径
    private static final String APP_LOG_BASE_DIR = "log";
    private static final String LSPD_COPY_DIR = "lspd";
    private static final String LSPD_LOG_SUBDIR = "log";
    private static final String LSPD_LOG_OLD_SUBDIR = "log.old";
    private static final String FILTERED_DIR = "hyperceiler_filtered";
    private static final String LOG_FILE_PATTERN = "*";
    private static final String FILTERED_LOG_PREFIX = "hyperceiler_";
    private static final String ROTATION_MARKER = ".rotation_marker";

    private static final Pattern TIME_PATTERN = Pattern.compile("\\[\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final String HYPERCEILER_TAG = "HyperCeiler";

    private final Context context;
    private final File appLogBaseDir;           // files/log
    private final File lspdCopyBaseDir;         // files/log/lspd
    private final File lspdLogDir;              // files/log/lspd/log
    private final File lspdLogOldDir;           // files/log/lspd/log.old
    private final File filteredDir;             // files/log/hyperceiler_filtered
    private final File rotationMarkerFile;      // files/log/.rotation_marker

    private XposedLogLoader(Context context) {
        this.context = context.getApplicationContext();
        this.appLogBaseDir = new File(this.context.getFilesDir(), APP_LOG_BASE_DIR);
        this.lspdCopyBaseDir = new File(appLogBaseDir, LSPD_COPY_DIR);
        this.lspdLogDir = new File(lspdCopyBaseDir, LSPD_LOG_SUBDIR);
        this.lspdLogOldDir = new File(lspdCopyBaseDir, LSPD_LOG_OLD_SUBDIR);
        this.filteredDir = new File(appLogBaseDir, FILTERED_DIR);
        this.rotationMarkerFile = new File(appLogBaseDir, ROTATION_MARKER);
        initLogDirectories();
    }

    public static XposedLogLoader getInstance(Context context) {
        if (instance == null) {
            synchronized (XposedLogLoader.class) {
                if (instance == null) {
                    instance = new XposedLogLoader(context);
                }
            }
        }
        return instance;
    }

    public static void loadLogs(Runnable callback) {
        Context appContext = getApplicationContext();
        if (appContext == null) {
            Log.e(TAG, "Application context is null");
            if (callback != null) callback.run();
            return;
        }
        getInstance(appContext).syncLogs(callback);
    }

    public static void loadLogsSync() {
        Context appContext = getApplicationContext();
        if (appContext == null) {
            Log.e(TAG, "Application context is null");
            return;
        }
        getInstance(appContext).syncLogsSync();
    }

    public static File exportLogsStatic(Context context) {
        return getInstance(context).exportLogs();
    }

    public static void clearLogsStatic(Context context) {
        getInstance(context).clearLogs();
    }

    private void initLogDirectories() {
        if (!appLogBaseDir.exists() && !appLogBaseDir.mkdirs()) {
            Log.w(TAG, "Failed to create app log base directory");
        }
        if (!lspdCopyBaseDir.exists() && !lspdCopyBaseDir.mkdirs()) {
            Log.w(TAG, "Failed to create lspd copy base directory");
        }
        if (!lspdLogDir.exists() && !lspdLogDir.mkdirs()) {
            Log.w(TAG, "Failed to create lspd log directory");
        }
        if (!lspdLogOldDir.exists() && !lspdLogOldDir.mkdirs()) {
            Log.w(TAG, "Failed to create lspd log old directory");
        }
        if (!filteredDir.exists() && !filteredDir.mkdirs()) {
            Log.w(TAG, "Failed to create filtered directory");
        }

        File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
        if (!filteredLogDir.exists() && !filteredLogDir.mkdirs()) {
            Log.w(TAG, "Failed to create filtered log directory");
        }
        File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
        if (!filteredLogOldDir.exists() && !filteredLogOldDir.mkdirs()) {
            Log.w(TAG, "Failed to create filtered log old directory");
        }
    }

    public void syncLogs(Runnable callback) {
        new Thread(() -> {
            try {
                // 先同步日志文件
                syncLogsInternal();
                // 同步完成后加载到内存
                loadFilteredLogsToMemory();
                Log.i(TAG, "Log sync completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync logs", e);
            } finally {
                if (callback != null) {
                    callback.run();
                }
            }
        }).start();
    }

    public void syncLogsSync() {
        try {
            syncLogsInternal();
            loadFilteredLogsToMemory();
            Log.i(TAG, "Log sync completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync logs", e);
        }
    }

    private void syncLogsInternal() {
        rotateLogsIfNeeded();
        copyLspdLogs();
        filterAndSaveLogs();
    }

    private void rotateLogsIfNeeded() {
        String checkCmd = "[ -d '" + LSPD_LOG_OLD_DIR + "' ] && echo 'exists' || echo 'not_exists'";
        String result = ShellUtils.rootExecCmd(checkCmd).trim();

        if ("exists".equals(result)) {
            String lspdOldTimeCmd = "stat -c %Y '" + LSPD_LOG_OLD_DIR + "' 2>/dev/null";
            String lspdOldTime = ShellUtils.rootExecCmd(lspdOldTimeCmd).trim();
            long lastRotationTime = readRotationMarker();

            try {
                long currentLspdOldTime = Long.parseLong(lspdOldTime);
                if (currentLspdOldTime > lastRotationTime) {
                    rotateAppLogs();
                    writeRotationMarker(currentLspdOldTime);

                    try {
                        LogManager.getInstance().rotateAppLogs();
                        Log.i(TAG, "App logs rotated along with Xposed logs");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to rotate app logs", e);
                    }
                    Log.i(TAG, "Logs rotated based on LSPosed rotation");
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Failed to parse LSPosed log.old time", e);
            }
        } else {
            if (rotationMarkerFile.exists()) {
                rotationMarkerFile.delete();
            }
        }
    }

    private long readRotationMarker() {
        if (!rotationMarkerFile.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(rotationMarkerFile))) {
            String line = reader.readLine();
            return line != null ? Long.parseLong(line.trim()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeRotationMarker(long timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rotationMarkerFile))) {
            writer.write(String.valueOf(timestamp));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write rotation marker", e);
        }
    }

    private void rotateAppLogs() {
        try {
            deleteDirectory(lspdLogOldDir);
            deleteDirectory(new File(filteredDir, LSPD_LOG_OLD_SUBDIR));

            if (lspdLogDir.exists() && hasFiles(lspdLogDir)) {
                File tempOldDir = new File(lspdCopyBaseDir, LSPD_LOG_OLD_SUBDIR);
                if (!lspdLogDir.renameTo(tempOldDir)) {
                    Log.w(TAG, "Failed to rename lspd log directory to log.old");
                }
                File parentDir = lspdLogOldDir.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    Log.w(TAG, "Failed to create parent directory for lspd log old");
                }
            }

            File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
            if (filteredLogDir.exists() && hasFiles(filteredLogDir)) {
                if (!filteredLogDir.renameTo(filteredLogOldDir)) {
                    Log.w(TAG, "Failed to rename filtered log directory to log.old");
                }
            }

            if (!lspdLogDir.exists() && !lspdLogDir.mkdirs()) {
                Log.w(TAG, "Failed to recreate lspd log directory");
            }

            File newFilteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            if (!newFilteredLogDir.exists() && !newFilteredLogDir.mkdirs()) {
                Log.w(TAG, "Failed to recreate filtered log directory");
            }

            Log.i(TAG, "Xposed logs rotated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate Xposed logs", e);
        }
    }

    private boolean hasFiles(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    private void copyLspdLogs() {
        copyLspdDirectory(LSPD_LOG_DIR, lspdLogDir);
        copyLspdDirectory(LSPD_LOG_OLD_DIR, lspdLogOldDir);
    }

    private void copyLspdDirectory(String sourceDir, File targetDir) {
        try {
            String checkCmd = "[ -d '" + sourceDir + "' ] && echo 'exists' || echo 'not_exists'";
            String checkResult = ShellUtils.rootExecCmd(checkCmd).trim();

            if (!"exists".equals(checkResult)) {
                Log.w(TAG, "Source directory does not exist: " + sourceDir);
                return;
            }

            targetDir.mkdirs();

            String copyCmd = "cp -f " + sourceDir + "/* '" + targetDir.getAbsolutePath() + "/' 2>/dev/null";
            ShellUtils.rootExecCmd(copyCmd);

            String chmodCmd = "chmod -R 644 '" + targetDir.getAbsolutePath() + "'/* 2>/dev/null";
            ShellUtils.rootExecCmd(chmodCmd);

            Log.d(TAG, "Copied logs from " + sourceDir + " to " + targetDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy directory: " + sourceDir, e);
        }
    }

    private void filterAndSaveLogs() {
        File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
        filteredLogDir.mkdirs();
        filterLogsInDirectory(lspdLogDir, filteredLogDir);

        File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
        filteredLogOldDir.mkdirs();
        filterLogsInDirectory(lspdLogOldDir, filteredLogOldDir);
    }

    private void filterLogsInDirectory(File sourceDir, File targetDir) {
        if (!sourceDir.exists()) return;

        File[] sourceFiles = sourceDir.listFiles((dir, name) -> name.startsWith("modules_") && name.endsWith(".log"));
        if (sourceFiles == null || sourceFiles.length == 0) return;

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(targetDir, FILTERED_LOG_PREFIX + fileName);
            try {
                filterAndAppendLog(sourceFile, targetFile);
            } catch (Exception e) {
                Log.e(TAG, "Failed to filter log: " + sourceFile.getName(), e);
            }
        }
    }

    private void filterAndAppendLog(File sourceFile, File targetFile) {
        try {
            long lastTimestamp = getLastTimestamp(targetFile);
            List<String> filteredEntries = filterLogFile(sourceFile, lastTimestamp);

            if (!filteredEntries.isEmpty()) {
                appendEntriesToFile(targetFile, filteredEntries);
                Log.d(TAG, "Filtered " + filteredEntries.size() + " entries from: " + sourceFile.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter log: " + sourceFile.getName(), e);
        }
    }

    private List<String> filterLogFile(File sourceFile, long afterTimestamp) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            boolean isHyperCeilerEntry = false;
            boolean includeEntry = false;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (isHyperCeilerEntry && includeEntry && currentEntry.length() > 0) {
                        result.add(currentEntry.toString());
                    }
                    isHyperCeilerEntry = line.contains(HYPERCEILER_TAG);
                    long entryTimestamp = parseTimestamp(line);
                    includeEntry = entryTimestamp > afterTimestamp;
                    currentEntry = new StringBuilder(line);
                } else {
                    if (currentEntry.length() > 0) {
                        currentEntry.append("\n").append(line);
                    }
                }
            }
            if (isHyperCeilerEntry && includeEntry && currentEntry.length() > 0) {
                result.add(currentEntry.toString());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file: " + sourceFile.getName(), e);
        }
        return result;
    }

    private long getLastTimestamp(File file) {
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String lastTimestampLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    lastTimestampLine = line;
                }
            }
            if (lastTimestampLine != null) {
                return parseTimestamp(lastTimestampLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read last timestamp from: " + file.getName(), e);
        }
        return 0;
    }

    private void appendEntriesToFile(File file, List<String> entries) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            for (String entry : entries) {
                writer.write(entry);
                writer.write("\n");
            }
        }
    }

    private void loadFilteredLogsToMemory() {
        try {
            LogManager logManager = LogManager.getInstance();
            List<LogEntry> entries = new ArrayList<>();

            File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
            entries.addAll(loadLogsFromDirectory(filteredLogOldDir));

            File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            entries.addAll(loadLogsFromDirectory(filteredLogDir));

            // 按时间排序
            entries.sort(Comparator.comparingLong(LogEntry::getTimestamp));

            // 先清空再添加，确保数据一致性
            synchronized (LogManager.class) {
                logManager.clearXposedLogs();
                if (!entries.isEmpty()) {
                    logManager.addXposedLogs(entries);
                    Log.i(TAG, "Loaded " + entries.size() + " Xposed log entries to memory");
                } else {
                    Log.i(TAG, "No Xposed log entries found");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load filtered logs to memory", e);
        }
    }

    private List<LogEntry> loadLogsFromDirectory(File directory) {
        List<LogEntry> entries = new ArrayList<>();
        if (!directory.exists()) return entries;

        File[] files = directory.listFiles((dir, name) ->name.startsWith(FILTERED_LOG_PREFIX) && name.endsWith(".log"));

        if (files == null || files.length == 0) return entries;

        Arrays.sort(files, Comparator.comparing(File::getName));

        int estimatedSize = files.length * 100;
        entries = new ArrayList<>(estimatedSize);

        for (File file : files) {
            try {
                parseLogFileOptimized(file, entries);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse log file: " + file.getName(), e);
            }
        }
        return entries;
    }

    private void parseLogFileOptimized(File file, List<LogEntry> entries) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            String line;
            LogEntry currentEntry = null;
            StringBuilder currentMessage = null;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (currentEntry != null && currentMessage != null) {
                        entries.add(buildLogEntry(currentEntry, currentMessage.toString()));
                    }
                    currentEntry = parseXposedLogLine(line);
                    currentMessage = new StringBuilder(256);
                    currentMessage.append(currentEntry.getMessage());
                } else if (currentMessage != null) {
                    currentMessage.append("\n").append(line);
                }
            }
            if (currentEntry != null && currentMessage != null) {
                entries.add(buildLogEntry(currentEntry, currentMessage.toString()));
            }
        }
    }

    public File exportLogs() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String zipFileName = "hyperceiler_logs_" + timestamp + ".zip";
            File zipFile = new File(context.getExternalFilesDir(null), zipFileName);

            String cmd = "cd '" + context.getFilesDir().getAbsolutePath() + "' && " +
                "zip -r '" + zipFile.getAbsolutePath() + "' '" + APP_LOG_BASE_DIR + "'";

            ShellUtils.execCommand(cmd, checkRootPermission() == 0);

            if (zipFile.exists()) {
                Log.i(TAG, "Logs exported to: " + zipFile.getAbsolutePath());
                return zipFile;
            } else {
                Log.e(TAG, "Failed to create zip file");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to export logs", e);
            return null;
        }
    }

    public void clearLogs() {
        deleteDirectory(lspdCopyBaseDir);
        deleteDirectory(filteredDir);
        if (rotationMarkerFile.exists() && !rotationMarkerFile.delete()) {
            Log.w(TAG, "Failed to delete rotation marker file");
        }
        initLogDirectories();
        try {
            LogManager.getInstance().clearXposedLogs();
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear Xposed logs from LogManager", e);
        }
    }

    private boolean isNewLogEntry(String line) {
        return line.startsWith("[") && TIME_PATTERN.matcher(line).find();
    }

    private long parseTimestamp(String line) {
        Matcher matcher = TIME_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                return TIME_FORMAT.parse(matcher.group(1)).getTime();
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private LogEntry parseXposedLogLine(String line) {
        long timestamp = parseTimestamp(line);
        String level = parseLevel(line);
        String message = extractMessage(line);
        String tag = extractPackageName(message);

        if (message.contains("[CrashMonitor]")) {
            level = "C";
        }

        return new LogEntry(timestamp, level, "Xposed", message, tag, true);
    }


    private String parseLevel(String line) {
        if (line.contains("[E]")) return "E";
        if (line.contains("[W]")) return "W";
        if (line.contains("[I]")) return "I";
        if (line.contains("[D]")) return "D";
        return "C";
    }

    private String extractMessage(String line) {
        int tagIndex = line.indexOf("[" + HYPERCEILER_TAG + "]");
        return tagIndex != -1 ? line.substring(tagIndex) : line;
    }

    private String extractPackageName(String message) {
        for (String level : new String[]{"[I]", "[D]", "[W]", "[E]", "[C]"}) {
            int levelIndex = message.indexOf(level);
            if (levelIndex != -1) {
                int startIndex = levelIndex + level.length();
                if (startIndex < message.length() && message.charAt(startIndex) == '[') {
                    int endIndex = message.indexOf("]", startIndex + 1);
                    if (endIndex != -1) {
                        String candidate = message.substring(startIndex + 1, endIndex);
                        if (isValidPackageName(candidate)) {
                            return candidate;
                        }
                    }
                }
                break;
            }
        }
        return "Other";
    }

    private boolean isValidPackageName(String name) {
        if (name == null || name.isEmpty()) return false;
        if ("system".equals(name)) return true;
        if (!name.contains(".")) return false;
        for (String part : name.split("\\.")) {
            if (part.isEmpty() || !Character.isLetter(part.charAt(0))) return false;
            for (char c : part.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') return false;
            }
        }
        return true;
    }

    private LogEntry buildLogEntry(LogEntry template, String message) {
        return new LogEntry(
            template.getTimestamp(),
            template.getLevel(),
            template.getModule(),
            message.trim(),
            template.getTag(),
            template.isNewLine()
        );
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            if (!directory.delete()) {
                Log.w(TAG, "Failed to delete directory: " + directory.getAbsolutePath());
            }
        }
    }

    @SuppressLint("PrivateApi")
    private static Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object app = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            return (Context) app;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get application context", e);
            return null;
        }
    }
}
