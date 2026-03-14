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
package com.sevtinge.hyperceiler.logviewer;

import static com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.checkRootPermission;

import android.annotation.SuppressLint;
import android.content.Context;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
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

    // LSPosed 日志源路径
    private static final String LSPD_LOG_DIR = "/data/adb/lspd/log";
    private static final String LSPD_LOG_OLD_DIR = "/data/adb/lspd/log.old";

    // 应用私有日志路径
    private static final String APP_LOG_BASE_DIR = "log";
    private static final String LSPD_COPY_DIR = "lspd";
    private static final String LSPD_LOG_SUBDIR = "log";
    private static final String LSPD_LOG_OLD_SUBDIR = "log.old";
    private static final String FILTERED_DIR = "hyperceiler_filtered";
    private static final String FILTERED_LOG_PREFIX = "hyperceiler_";
    private static final String ROTATION_MARKER = ".rotation_marker";

    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\[\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    private static final SimpleDateFormat TIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final String HYPERCEILER_TAG = "HyperCeiler";

    private static final Pattern LINE_PREFIX_LEVEL_PATTERN = Pattern.compile(
        "\\[\\s*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\d+:\\s*\\d+:\\s*\\d+\\s+([VDIWEC])/");

    private static final Pattern NEW_MODULE_PATTERN = Pattern.compile(
        "\\[([^,\\]]+),([^\\]]+)\\]");

    // 分页常量
    private static final long PAGE_SIZE_BYTES = 4L * 1024 * 1024;
    private static final String PAGE_MARKER_PREFIX = "----part ";
    private static final String PAGE_MARKER_SUFFIX = " start----";
    private static final int MAX_ENTRY_MESSAGE_LENGTH = 32 * 1024;

    private final Context context;
    private final File appLogBaseDir;
    private final File lspdCopyBaseDir;
    private final File lspdLogDir;
    private final File lspdLogOldDir;
    private final File filteredDir;
    private final File rotationMarkerFile;

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
            AndroidLog.e(TAG, "Application context is null");
            if (callback != null) callback.run();
            return;
        }
        getInstance(appContext).syncLogs(callback);
    }

    public static void loadLogsSync() {
        Context appContext = getApplicationContext();
        if (appContext == null) {
            AndroidLog.e(TAG, "Application context is null");
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
        ensureDirs(appLogBaseDir, lspdCopyBaseDir, lspdLogDir, lspdLogOldDir, filteredDir,
            new File(filteredDir, LSPD_LOG_SUBDIR),
            new File(filteredDir, LSPD_LOG_OLD_SUBDIR));
    }

    // ==================== 同步入口 ====================

    public void syncLogs(Runnable callback) {
        new Thread(() -> {
            try {
                syncLogsInternal();
                loadFilteredLogsToMemory();
                AndroidLog.d(TAG, "Log sync completed successfully");
            } catch (OutOfMemoryError e) {
                AndroidLog.e(TAG, "OOM during log sync", e);
            } catch (Exception e) {
                AndroidLog.e(TAG, "Failed to sync logs", e);
            } finally {
                if (callback != null) callback.run();
            }
        }).start();
    }

    public void syncLogsSync() {
        try {
            syncLogsInternal();
            loadFilteredLogsToMemory();
            AndroidLog.d(TAG, "Log sync completed successfully");
        } catch (OutOfMemoryError e) {
            AndroidLog.e(TAG, "OOM during log sync", e);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to sync logs", e);
        }
    }

    private void syncLogsInternal() {
        rotateLogsIfNeeded();
        copyLspdLogs();
        filterAndSaveLogs();
    }

    // ==================== 日志轮转 ====================

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
                        AndroidLog.d(TAG, "App logs rotated along with Xposed logs");
                    } catch (Exception e) {
                        AndroidLog.w(TAG, "Failed to rotate app logs", e);
                    }
                    AndroidLog.d(TAG, "Logs rotated based on LSPosed rotation");
                }
            } catch (NumberFormatException e) {
                AndroidLog.w(TAG, "Failed to parse LSPosed log.old time", e);
            }
        } else {
            if (rotationMarkerFile.exists()) rotationMarkerFile.delete();
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
            AndroidLog.e(TAG, "Failed to write rotation marker", e);
        }
    }

    private void rotateAppLogs() {
        try {
            deleteDirectory(lspdLogOldDir);
            deleteDirectory(new File(filteredDir, LSPD_LOG_OLD_SUBDIR));

            if (lspdLogDir.exists() && hasFiles(lspdLogDir)) {
                File tempOldDir = new File(lspdCopyBaseDir, LSPD_LOG_OLD_SUBDIR);
                if (!lspdLogDir.renameTo(tempOldDir)) {
                    AndroidLog.w(TAG, "Failed to rename lspd log directory to log.old");
                }
                File parentDir = lspdLogOldDir.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    AndroidLog.w(TAG, "Failed to create parent directory for lspd log old");
                }
            }

            File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
            if (filteredLogDir.exists() && hasFiles(filteredLogDir)) {
                if (!filteredLogDir.renameTo(filteredLogOldDir)) {
                    AndroidLog.w(TAG, "Failed to rename filtered log directory to log.old");
                }
            }

            if (!lspdLogDir.exists() && !lspdLogDir.mkdirs()) {
                AndroidLog.w(TAG, "Failed to recreate lspd log directory");
            }
            File newFilteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            if (!newFilteredLogDir.exists() && !newFilteredLogDir.mkdirs()) {
                AndroidLog.w(TAG, "Failed to recreate filtered log directory");
            }
            AndroidLog.d(TAG, "Xposed logs rotated");
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to rotate Xposed logs", e);
        }
    }

    private boolean hasFiles(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    // ==================== 拷贝（当前 + 旧的都拷） ====================

    private void copyLspdLogs() {
        copyLspdDirectory(LSPD_LOG_DIR, lspdLogDir);
        copyLspdDirectory(LSPD_LOG_OLD_DIR, lspdLogOldDir);
    }

    private void copyLspdDirectory(String sourceDir, File targetDir) {
        try {
            String checkCmd = "[ -d '" + sourceDir + "' ] && echo 'exists' || echo 'not_exists'";
            if (!"exists".equals(ShellUtils.rootExecCmd(checkCmd).trim())) {
                AndroidLog.w(TAG, "Source directory does not exist: " + sourceDir);
                return;
            }
            targetDir.mkdirs();
            ShellUtils.rootExecCmd("cp -f " + sourceDir + "/* '" + targetDir.getAbsolutePath() + "/' 2>/dev/null");
            ShellUtils.rootExecCmd("chmod -R 644 '" + targetDir.getAbsolutePath() + "'/* 2>/dev/null");
            AndroidLog.d(TAG, "Copied logs from " + sourceDir + " to " + targetDir.getAbsolutePath());
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to copy directory: " + sourceDir, e);
        }
    }

    // ==================== 过滤 + 分页写入（当前 + 旧的都过滤存储） ====================

    private void filterAndSaveLogs() {
        File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
        filteredLogDir.mkdirs();
        filterLogsInDirectory(lspdLogDir, filteredLogDir);

        File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
        filteredLogOldDir.mkdirs();
        filterLogsInDirectory(lspdLogOldDir, filteredLogOldDir);
    }

    /**
     * 优先 modules_，过滤后无内容则 fallback 到 verbose_
     */
    private void filterLogsInDirectory(File sourceDir, File targetDir) {
        if (!sourceDir.exists()) return;

        File[] modulesFiles = sourceDir.listFiles((d, n) ->
            n.startsWith("modules_") && n.endsWith(".log"));
        int count = 0;
        if (modulesFiles != null && modulesFiles.length > 0) {
            count = filterFiles(modulesFiles, targetDir);
        }
        if (count == 0) {
            File[] verboseFiles = sourceDir.listFiles((d, n) ->
                n.startsWith("verbose_") && n.endsWith(".log"));
            if (verboseFiles != null && verboseFiles.length > 0) {
                filterFiles(verboseFiles, targetDir);
            }
        }
    }

    private int filterFiles(File[] sourceFiles, File targetDir) {
        int total = 0;
        for (File src : sourceFiles) {
            File dst = new File(targetDir, FILTERED_LOG_PREFIX + src.getName());
            try {
                total += filterAndAppendLog(src, dst);
            } catch (Exception e) {
                AndroidLog.e(TAG, "Failed to filter log: " + src.getName(), e);
            }
        }
        return total;
    }

    private int filterAndAppendLog(File sourceFile, File targetFile) {
        try {
            long lastTimestamp = getLastTimestamp(targetFile);
            List<String> filtered = filterLogFile(sourceFile, lastTimestamp);
            if (!filtered.isEmpty()) {
                appendEntriesWithPaging(targetFile, filtered);
                AndroidLog.d(TAG, "Filtered " + filtered.size() + " entries from: " + sourceFile.getName());
            }
            return filtered.size();
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to filter log: " + sourceFile.getName(), e);
            return 0;
        }
    }

    private List<String> filterLogFile(File sourceFile, long afterTimestamp) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            boolean isTarget = false;
            boolean include = false;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (isTarget && include && !currentEntry.isEmpty()) {
                        result.add(currentEntry.toString());
                    }
                    isTarget = line.contains(HYPERCEILER_TAG);
                    include = parseTimestamp(line) > afterTimestamp;
                    currentEntry = new StringBuilder(line);
                } else if (!currentEntry.isEmpty()
                    && currentEntry.length() + line.length() + 1 <= MAX_ENTRY_MESSAGE_LENGTH) {
                    currentEntry.append("\n").append(line);
                }
            }
            if (isTarget && include && !currentEntry.isEmpty()) {
                result.add(currentEntry.toString());
            }
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to read log file: " + sourceFile.getName(), e);
        }
        return result;
    }

    private long getLastTimestamp(File file) {
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String last = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) last = line;
            }
            if (last != null) return parseTimestamp(last);
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to read last timestamp from: " + file.getName(), e);
        }
        return 0;
    }

    // ==================== 分页写入 ====================

    private void appendEntriesWithPaging(File file, List<String> entries) throws IOException {
        boolean isNew = !file.exists() || file.length() == 0;
        int pageNum = isNew ? 1 : getCurrentPage(file);
        long pageSize = isNew ? 0 : getLastPageSize(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (isNew) {
                String marker = PAGE_MARKER_PREFIX + pageNum + PAGE_MARKER_SUFFIX + "\n";
                writer.write(marker);
                pageSize = marker.length();
            }
            for (String entry : entries) {
                int len = entry.length() + 1;
                if (pageSize + len > PAGE_SIZE_BYTES) {
                    pageNum++;
                    String marker = PAGE_MARKER_PREFIX + pageNum + PAGE_MARKER_SUFFIX + "\n";
                    writer.write(marker);
                    pageSize = marker.length();
                }
                writer.write(entry);
                writer.write("\n");
                pageSize += len;
            }
        }
    }

    private int getCurrentPage(File file) {
        int page = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isPageMarker(line)) {
                    try {
                        page = Integer.parseInt(
                            line.substring(PAGE_MARKER_PREFIX.length(),
                                line.length() - PAGE_MARKER_SUFFIX.length()).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            AndroidLog.w(TAG, "Failed to read current page from: " + file.getName(), e);
        }
        return page;
    }

    private long getLastPageSize(File file) {
        long size = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isPageMarker(line)) size = 0;
                size += line.length() + 1;
            }
        } catch (IOException e) {
            AndroidLog.w(TAG, "Failed to calculate last page size: " + file.getName(), e);
        }
        return size;
    }

    private boolean isPageMarker(String line) {
        return line.startsWith(PAGE_MARKER_PREFIX) && line.endsWith(PAGE_MARKER_SUFFIX);
    }

    // ==================== 加载到内存（只读最新一页） ====================

    /**
     * 只从 filtered/log（当前）中读取最新一页加载到内存显示。
     * filtered/log.old 已同步存储在本地但不加载到内存。
     */
    private void loadFilteredLogsToMemory() {
        try {
            LogManager logManager = LogManager.getInstance();

            // 只读当前 filtered/log 的最新一页
            File filteredLogDir = new File(filteredDir, LSPD_LOG_SUBDIR);
            List<LogEntry> entries = loadLastPageFromDirectory(filteredLogDir);

            // 当前目录无内容时，fallback 读 filtered/log.old 的最新一页
            if (entries.isEmpty()) {
                File filteredLogOldDir = new File(filteredDir, LSPD_LOG_OLD_SUBDIR);
                entries = loadLastPageFromDirectory(filteredLogOldDir);
            }

            entries.sort(Comparator.comparingLong(LogEntry::getTimestamp));

            synchronized (LogManager.class) {
                logManager.clearXposedLogs();
                if (!entries.isEmpty()) {
                    logManager.addXposedLogs(entries);
                    AndroidLog.d(TAG, "Loaded " + entries.size() + " Xposed log entries to memory");
                } else {
                    AndroidLog.d(TAG, "No Xposed log entries found");
                }
                logManager.setXposedLogsLoaded(true);
            }
        } catch (OutOfMemoryError e) {
            AndroidLog.e(TAG, "OOM while loading filtered logs to memory", e);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to load filtered logs to memory", e);
        } finally {
            try {
                LogManager.getInstance().setXposedLogsLoaded(true);
            } catch (Exception e) {
                AndroidLog.w(TAG, "Failed to mark Xposed logs as loaded", e);
            }
        }
    }

    private List<LogEntry> loadLastPageFromDirectory(File directory) {
        List<LogEntry> entries = new ArrayList<>();
        if (!directory.exists()) return entries;

        File[] files = directory.listFiles((d, n) ->
            n.startsWith(FILTERED_LOG_PREFIX) && n.endsWith(".log"));
        if (files == null || files.length == 0) return entries;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            try {
                parseLastPage(file, entries);
            } catch (OutOfMemoryError e) {
                AndroidLog.e(TAG, "OOM parsing log file: " + file.getName(), e);
                break;
            } catch (Exception e) {
                AndroidLog.e(TAG, "Failed to parse log file: " + file.getName(), e);
            }
        }
        return entries;
    }

    /**
     * 两遍扫描：第一遍定位最后一个 page marker，第二遍只解析该页
     */
    private void parseLastPage(File file, List<LogEntry> entries) throws IOException {
        long lastMarkerOffset = 0;
        long offset = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isPageMarker(line)) lastMarkerOffset = offset;
                offset += line.length() + 1;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            long skipped = 0;
            while (skipped < lastMarkerOffset) {
                String line = reader.readLine();
                if (line == null) break;
                skipped += line.length() + 1;
            }

            LogEntry currentEntry = null;
            StringBuilder currentMessage = null;
            String line;

            while ((line = reader.readLine()) != null) {
                if (isPageMarker(line)) continue;

                if (isNewLogEntry(line)) {
                    if (currentEntry != null && currentMessage != null) {
                        entries.add(buildLogEntry(currentEntry, currentMessage.toString()));
                    }
                    currentEntry = parseXposedLogLine(line);
                    currentMessage = new StringBuilder(256);
                    currentMessage.append(currentEntry.getMessage());
                } else if (currentMessage != null
                    && currentMessage.length() + line.length() + 1 <= MAX_ENTRY_MESSAGE_LENGTH) {
                    currentMessage.append("\n").append(line);
                }
            }
            if (currentEntry != null && currentMessage != null) {
                entries.add(buildLogEntry(currentEntry, currentMessage.toString()));
            }
        }
    }

    // ==================== 导出 / 清理 ====================

    public File exportLogs() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File zipFile = new File(context.getExternalFilesDir(null),
                "hyperceiler_logs_" + timestamp + ".zip");
            String cmd = "cd '" + context.getFilesDir().getAbsolutePath() + "' && " +
                "zip -r '" + zipFile.getAbsolutePath() + "' '" + APP_LOG_BASE_DIR + "'";
            ShellUtils.execCommand(cmd, checkRootPermission() == 0);
            if (zipFile.exists()) {
                AndroidLog.d(TAG, "Logs exported to: " + zipFile.getAbsolutePath());
                return zipFile;
            }
            AndroidLog.e(TAG, "Failed to create zip file");
            return null;
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to export logs", e);
            return null;
        }
    }

    public void clearLogs() {
        deleteDirectory(lspdCopyBaseDir);
        deleteDirectory(filteredDir);
        if (rotationMarkerFile.exists() && !rotationMarkerFile.delete()) {
            AndroidLog.w(TAG, "Failed to delete rotation marker file");
        }
        initLogDirectories();
        try {
            LogManager.getInstance().clearXposedLogs();
            LogManager.getInstance().setXposedLogsLoaded(true);
        } catch (Exception e) {
            AndroidLog.w(TAG, "Failed to clear Xposed logs from LogManager", e);
        }
    }

    // ==================== 日志行解析 ====================

    private boolean isNewLogEntry(String line) {
        return line.startsWith("[") && TIME_PATTERN.matcher(line).find();
    }

    private long parseTimestamp(String line) {
        Matcher matcher = TIME_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                return TIME_FORMAT.parse(matcher.group(1)).getTime();
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private boolean isNewFormat(String line) {
        return NEW_MODULE_PATTERN.matcher(line).find();
    }

    private String parseLevel(String line) {
        // 优先从行前缀读取
        Matcher prefixMatcher = LINE_PREFIX_LEVEL_PATTERN.matcher(line);
        if (prefixMatcher.find()) {
            String level = prefixMatcher.group(1);
            if (level != null && !level.isEmpty() && isNewFormat(line)) {
                return level;
            }
        }
        if (line.contains("[E]")) return "E";
        if (line.contains("[W]")) return "W";
        if (line.contains("[I]")) return "I";
        if (line.contains("[D]")) return "D";
        return "I";
    }

    private String extractMessage(String line) {
        // 新版: 找 [xxx,HyperCeiler] 之后的部分
        Matcher moduleMatcher = NEW_MODULE_PATTERN.matcher(line);
        if (moduleMatcher.find()) {
            int i = moduleMatcher.end();
            while (i < line.length() && line.charAt(i) == ' ') i++;
            return i < line.length() ? line.substring(i) : line;
        }
        int tagIndex = line.indexOf("[" + HYPERCEILER_TAG + "]");
        return tagIndex != -1 ? line.substring(tagIndex) : line;
    }

    private String extractPackageName(String message) {
        // 旧版: 找级别标记 [I]/[D] 等，其后的 [xxx] 如果是包名就取
        for (String level : new String[]{"[I]", "[D]", "[W]", "[E]", "[C]"}) {
            int idx = message.indexOf(level);
            if (idx != -1) {
                int start = idx + level.length();
                if (start < message.length() && message.charAt(start) == '[') {
                    int end = message.indexOf("]", start + 1);
                    if (end != -1) {
                        String candidate = message.substring(start + 1, end);
                        if (isValidPackageName(candidate)) return candidate;
                    }
                }
                break;
            }
        }

        if (message.startsWith("[")) {
            int end = message.indexOf("]");
            if (end != -1) {
                String first = message.substring(1, end);
                if (isValidPackageName(first)) return first;
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

    /**
     * 解析 Xposed 日志行
     */
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

    // ==================== 工具方法 ====================

    private void ensureDirs(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists() && !dir.mkdirs()) {
                AndroidLog.w(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            }
        }
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
                            AndroidLog.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            if (!directory.delete()) {
                AndroidLog.w(TAG, "Failed to delete directory: " + directory.getAbsolutePath());
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
            AndroidLog.e(TAG, "Failed to get application context", e);
            return null;
        }
    }
}
