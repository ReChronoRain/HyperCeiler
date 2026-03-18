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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.log;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final String LSPD_LOG_DIR = "/data/adb/lspd/log";
    private static final String LSPD_LOG_OLD_DIR = "/data/adb/lspd/log.old";

    private static final String APP_LOG_BASE_DIR = "log";
    private static final String LSPD_COPY_DIR = "lspd";
    private static final String LSPD_LOG_SUBDIR = "log";
    private static final String LSPD_LOG_OLD_SUBDIR = "log.old";
    private static final String FILTERED_DIR = "hyperceiler_filtered";
    private static final String FILTERED_LOG_PREFIX = "hyperceiler_";
    private static final String ROTATION_MARKER = ".rotation_marker";

    private static final String HYPERCEILER_TAG = "HyperCeiler";
    private static final long PAGE_SIZE_BYTES = 4L * 1024 * 1024;
    private static final String PAGE_MARKER_PREFIX = "----part ";
    private static final String PAGE_MARKER_SUFFIX = " start----";
    private static final int MAX_ENTRY_MESSAGE_LENGTH = 32 * 1024;
    private static final int INSERT_BATCH_SIZE = 500;

    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\[\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})"
    );
    private static final SimpleDateFormat TIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final Pattern LINE_PREFIX_LEVEL_PATTERN = Pattern.compile(
        "\\[\\s*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+(\\d+):\\s*(\\d+):\\s*(\\d+)\\s+([VDIWEC])/"
    );
    private static final Pattern NEW_MODULE_PATTERN = Pattern.compile("\\[([^,\\]]+),([^\\]]+)\\]");

    private static volatile XposedLogLoader sInstance;

    private final Context mContext;
    private final File mAppLogBaseDir;
    private final File mLspdCopyBaseDir;
    private final File mLspdLogDir;
    private final File mLspdLogOldDir;
    private final File mFilteredDir;
    private final File mFilteredLogDir;
    private final File mFilteredLogOldDir;
    private final File mRotationMarkerFile;

    private XposedLogLoader(Context context) {
        mContext = context.getApplicationContext();
        mAppLogBaseDir = new File(mContext.getFilesDir(), APP_LOG_BASE_DIR);
        mLspdCopyBaseDir = new File(mAppLogBaseDir, LSPD_COPY_DIR);
        mLspdLogDir = new File(mLspdCopyBaseDir, LSPD_LOG_SUBDIR);
        mLspdLogOldDir = new File(mLspdCopyBaseDir, LSPD_LOG_OLD_SUBDIR);
        mFilteredDir = new File(mAppLogBaseDir, FILTERED_DIR);
        mFilteredLogDir = new File(mFilteredDir, LSPD_LOG_SUBDIR);
        mFilteredLogOldDir = new File(mFilteredDir, LSPD_LOG_OLD_SUBDIR);
        mRotationMarkerFile = new File(mAppLogBaseDir, ROTATION_MARKER);
        initLogDirectories();
    }

    private static XposedLogLoader getInstance(Context context) {
        if (sInstance == null) {
            synchronized (XposedLogLoader.class) {
                if (sInstance == null) {
                    sInstance = new XposedLogLoader(context);
                }
            }
        }
        return sInstance;
    }

    public static void syncLogsToDatabase(Context context) {
        new Thread(() -> syncLogsToDatabaseSync(context), "XposedLogSync").start();
    }

    public static synchronized void syncLogsToDatabaseSync(Context context) {
        if (context == null) {
            return;
        }
        getInstance(context).syncToDatabase();
    }

    public static void loadLogsSync() {
        try {
            syncLogsToDatabaseSync(LogRepository.getInstance().getContext());
        } catch (IllegalStateException e) {
            AndroidLog.w(TAG, "Skip Xposed log sync because LogRepository is not initialized yet", e);
        }
    }

    public static void loadLogsSync(Context context) {
        syncLogsToDatabaseSync(context);
    }

    public static synchronized void clearAllSync(Context context) {
        if (context == null) {
            return;
        }
        getInstance(context).clearLogs();
    }

    public static void clearAll(Context context) {
        new Thread(() -> clearAllSync(context), "XposedLogClear").start();
    }

    private void syncToDatabase() {
        if (ShellUtils.checkRootPermission() != 0) {
            AndroidLog.w(TAG, "Root permission is required to sync Xposed logs.");
            return;
        }

        syncLogsInternal();
        persistFilteredLogsToDatabase();
    }

    private void syncLogsInternal() {
        initLogDirectories();
        rotateLogsIfNeeded();
        copyLspdLogs();
        filterAndSaveLogs();
    }

    private void initLogDirectories() {
        ensureDirs(
            mAppLogBaseDir,
            mLspdCopyBaseDir,
            mLspdLogDir,
            mLspdLogOldDir,
            mFilteredDir,
            mFilteredLogDir,
            mFilteredLogOldDir
        );
    }

    private void rotateLogsIfNeeded() {
        String checkCmd = "[ -d '" + LSPD_LOG_OLD_DIR + "' ] && echo exists || echo missing";
        String result = ShellUtils.rootExecCmd(checkCmd).trim();
        if (!"exists".equals(result)) {
            if (mRotationMarkerFile.exists() && !mRotationMarkerFile.delete()) {
                AndroidLog.w(TAG, "Failed to delete rotation marker");
            }
            return;
        }

        String lspdOldTimeCmd = "stat -c %Y '" + LSPD_LOG_OLD_DIR + "' 2>/dev/null";
        String lspdOldTime = ShellUtils.rootExecCmd(lspdOldTimeCmd).trim();
        long lastRotationTime = readRotationMarker();
        try {
            long currentLspdOldTime = Long.parseLong(lspdOldTime);
            if (currentLspdOldTime > lastRotationTime) {
                rotateLocalLogs();
                writeRotationMarker(currentLspdOldTime);
            }
        } catch (NumberFormatException e) {
            AndroidLog.w(TAG, "Failed to parse LSPosed log.old time", e);
        }
    }

    private void rotateLocalLogs() {
        deleteDirectory(mLspdLogOldDir);
        deleteDirectory(mFilteredLogOldDir);

        if (mLspdLogDir.exists() && hasFiles(mLspdLogDir) && !mLspdLogDir.renameTo(mLspdLogOldDir)) {
            AndroidLog.w(TAG, "Failed to rotate local LSPosed log dir");
        }
        if (mFilteredLogDir.exists() && hasFiles(mFilteredLogDir) && !mFilteredLogDir.renameTo(mFilteredLogOldDir)) {
            AndroidLog.w(TAG, "Failed to rotate local filtered log dir");
        }

        ensureDirs(mLspdLogDir, mFilteredLogDir);
    }

    private void copyLspdLogs() {
        copyLspdDirectory(LSPD_LOG_DIR, mLspdLogDir);
        copyLspdDirectory(LSPD_LOG_OLD_DIR, mLspdLogOldDir);
    }

    private void copyLspdDirectory(String sourceDir, File targetDir) {
        String checkCmd = "[ -d '" + sourceDir + "' ] && echo exists || echo missing";
        if (!"exists".equals(ShellUtils.rootExecCmd(checkCmd).trim())) {
            clearDirectory(targetDir);
            return;
        }

        ensureDirs(targetDir);
        clearDirectory(targetDir);
        ShellUtils.rootExecCmd("cp -f '" + sourceDir + "'/* '" + targetDir.getAbsolutePath() + "/' 2>/dev/null");
        ShellUtils.rootExecCmd("chmod -R 644 '" + targetDir.getAbsolutePath() + "'/* 2>/dev/null");
    }

    private void filterAndSaveLogs() {
        filterLogsInDirectory(mLspdLogDir, mFilteredLogDir);
        filterLogsInDirectory(mLspdLogOldDir, mFilteredLogOldDir);
    }

    private void filterLogsInDirectory(File sourceDir, File targetDir) {
        clearDirectory(targetDir);
        ensureDirs(targetDir);

        if (!sourceDir.exists()) {
            return;
        }

        File[] modulesFiles = sourceDir.listFiles((dir, name) ->
            name.startsWith("modules_") && name.endsWith(".log")
        );
        if (modulesFiles != null && modulesFiles.length > 0) {
            filterFiles(modulesFiles, targetDir);
            return;
        }

        File[] verboseFiles = sourceDir.listFiles((dir, name) ->
            name.startsWith("verbose_") && name.endsWith(".log")
        );
        if (verboseFiles != null && verboseFiles.length > 0) {
            filterFiles(verboseFiles, targetDir);
        }
    }

    private void filterFiles(File[] sourceFiles, File targetDir) {
        Arrays.sort(sourceFiles, Comparator.comparing(File::getName));
        for (File sourceFile : sourceFiles) {
            File targetFile = new File(targetDir, FILTERED_LOG_PREFIX + sourceFile.getName());
            try {
                List<String> filteredEntries = filterLogFile(sourceFile);
                writePagedEntries(targetFile, filteredEntries);
            } catch (IOException e) {
                AndroidLog.e(TAG, "Failed to filter log: " + sourceFile.getName(), e);
            }
        }
    }

    private List<String> filterLogFile(File sourceFile) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile), 16384)) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            boolean isTarget = false;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (isTarget && !currentEntry.isEmpty()) {
                        result.add(currentEntry.toString());
                    }
                    isTarget = line.contains(HYPERCEILER_TAG);
                    currentEntry = new StringBuilder(line);
                } else if (!currentEntry.isEmpty()
                    && currentEntry.length() + line.length() + 1 <= MAX_ENTRY_MESSAGE_LENGTH) {
                    currentEntry.append('\n').append(line);
                }
            }

            if (isTarget && !currentEntry.isEmpty()) {
                result.add(currentEntry.toString());
            }
        }
        return result;
    }

    private void writePagedEntries(File targetFile, List<String> entries) throws IOException {
        if (entries.isEmpty()) {
            if (targetFile.exists() && !targetFile.delete()) {
                AndroidLog.w(TAG, "Failed to delete empty filtered log: " + targetFile.getAbsolutePath());
            }
            return;
        }

        int pageNum = 1;
        String marker = buildPageMarker(pageNum);
        long pageSize = utf8Size(marker);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, false), 16384)) {
            writer.write(marker);
            for (String entry : entries) {
                String entryWithNewline = entry + '\n';
                long entrySize = utf8Size(entryWithNewline);
                if (pageSize + entrySize > PAGE_SIZE_BYTES) {
                    pageNum++;
                    marker = buildPageMarker(pageNum);
                    writer.write(marker);
                    pageSize = utf8Size(marker);
                }
                writer.write(entryWithNewline);
                pageSize += entrySize;
            }
        }
    }

    private void persistFilteredLogsToDatabase() {
        LogDao dao = LogRepository.getInstance().getDao();
        dao.deleteByModule("Xposed");

        List<LogEntry> batch = new ArrayList<>(INSERT_BATCH_SIZE);
        importDirectoryToDatabase(mFilteredLogOldDir, dao, batch);
        importDirectoryToDatabase(mFilteredLogDir, dao, batch);
        if (!batch.isEmpty()) {
            dao.insertAll(batch);
            batch.clear();
        }
        dao.autoTrim();
    }

    private void importDirectoryToDatabase(File directory, LogDao dao, List<LogEntry> batch) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles((dir, name) ->
            name.startsWith(FILTERED_LOG_PREFIX) && name.endsWith(".log")
        );
        if (files == null || files.length == 0) {
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                importFilteredFile(file, dao, batch);
            } catch (IOException e) {
                AndroidLog.e(TAG, "Failed to import filtered log: " + file.getName(), e);
            }
        }
    }

    private void importFilteredFile(File file, LogDao dao, List<LogEntry> batch) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            LogEntry currentEntry = null;
            StringBuilder currentMessage = null;
            String line;

            while ((line = reader.readLine()) != null) {
                if (isPageMarker(line)) {
                    continue;
                }
                if (isNewLogEntry(line)) {
                    if (currentEntry != null && currentMessage != null) {
                        addBatchEntry(dao, batch, buildLogEntry(currentEntry, currentMessage.toString()));
                    }
                    currentEntry = parseXposedLogLine(line);
                    currentMessage = new StringBuilder(Math.max(256, currentEntry.getMessage().length()));
                    currentMessage.append(currentEntry.getMessage());
                } else if (currentMessage != null
                    && currentMessage.length() + line.length() + 1 <= MAX_ENTRY_MESSAGE_LENGTH) {
                    currentMessage.append('\n').append(line);
                }
            }

            if (currentEntry != null && currentMessage != null) {
                addBatchEntry(dao, batch, buildLogEntry(currentEntry, currentMessage.toString()));
            }
        }
    }

    private void addBatchEntry(LogDao dao, List<LogEntry> batch, LogEntry entry) {
        batch.add(entry);
        if (batch.size() >= INSERT_BATCH_SIZE) {
            dao.insertAll(batch);
            batch.clear();
        }
    }

    private LogEntry parseXposedLogLine(String line) {
        long timestamp = parseTimestamp(line);
        String level = parseLevel(line);
        String message = extractMessage(line);
        String uidPid = extractUidPid(line);
        String tag = extractPackageName(message);

        if (message.contains("[CrashMonitor]")) {
            level = "C";
        }
        return new LogEntry("Xposed", level, tag, LogDisplayHelper.withUidPidMeta(message, uidPid), timestamp);
    }

    private LogEntry buildLogEntry(LogEntry template, String message) {
        return new LogEntry(
            "Xposed",
            template.getLevel(),
            template.getTag(),
            message.trim(),
            template.getTimestamp()
        );
    }

    private boolean isNewLogEntry(String line) {
        return line != null && line.startsWith("[") && TIME_PATTERN.matcher(line).find();
    }

    private boolean isPageMarker(String line) {
        return line != null && line.startsWith(PAGE_MARKER_PREFIX) && line.endsWith(PAGE_MARKER_SUFFIX);
    }

    private long parseTimestamp(String line) {
        Matcher matcher = TIME_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                Date date = TIME_FORMAT.parse(matcher.group(1));
                return date == null ? 0 : date.getTime();
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String parseLevel(String line) {
        Matcher prefixMatcher = LINE_PREFIX_LEVEL_PATTERN.matcher(line);
        if (prefixMatcher.find()) {
            String level = prefixMatcher.group(4);
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

    private String extractUidPid(String line) {
        Matcher prefixMatcher = LINE_PREFIX_LEVEL_PATTERN.matcher(line);
        if (!prefixMatcher.find()) {
            return "";
        }
        String uid = prefixMatcher.group(1);
        String pid = prefixMatcher.group(2);
        if (uid == null || pid == null || uid.isEmpty() || pid.isEmpty()) {
            return "";
        }
        return uid + ":" + pid;
    }

    private boolean isNewFormat(String line) {
        return NEW_MODULE_PATTERN.matcher(line).find();
    }

    private String extractMessage(String line) {
        Matcher moduleMatcher = NEW_MODULE_PATTERN.matcher(line);
        if (moduleMatcher.find()) {
            int start = moduleMatcher.end();
            while (start < line.length() && line.charAt(start) == ' ') {
                start++;
            }
            return start < line.length() ? line.substring(start) : line;
        }
        int tagIndex = line.indexOf("[" + HYPERCEILER_TAG + "]");
        return tagIndex != -1 ? line.substring(tagIndex) : line;
    }

    private String extractPackageName(String message) {
        for (String level : new String[]{"[I]", "[D]", "[W]", "[E]", "[C]"}) {
            int index = message.indexOf(level);
            if (index == -1) {
                continue;
            }
            int start = index + level.length();
            if (start < message.length() && message.charAt(start) == '[') {
                int end = message.indexOf(']', start + 1);
                if (end != -1) {
                    String candidate = message.substring(start + 1, end);
                    if (isValidPackageName(candidate)) {
                        return candidate;
                    }
                }
            }
            break;
        }

        if (message.startsWith("[")) {
            int end = message.indexOf(']');
            if (end != -1) {
                String candidate = message.substring(1, end);
                if (isValidPackageName(candidate)) {
                    return candidate;
                }
            }
        }
        return LogDisplayHelper.OTHER_TAG_VALUE;
    }

    private boolean isValidPackageName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if ("system".equals(name)) {
            return true;
        }
        if (!name.contains(".")) {
            return false;
        }
        for (String part : name.split("\\.")) {
            if (part.isEmpty() || !Character.isLetter(part.charAt(0))) {
                return false;
            }
            for (char c : part.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return false;
                }
            }
        }
        return true;
    }

    private long readRotationMarker() {
        if (!mRotationMarkerFile.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(mRotationMarkerFile))) {
            String line = reader.readLine();
            return line == null ? 0 : Long.parseLong(line.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeRotationMarker(long timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mRotationMarkerFile, false))) {
            writer.write(String.valueOf(timestamp));
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to write rotation marker", e);
        }
    }

    private void clearLogs() {
        LogRepository.getInstance().getDao().deleteByModule("Xposed");
        deleteDirectory(mLspdCopyBaseDir);
        deleteDirectory(mFilteredDir);
        if (mRotationMarkerFile.exists() && !mRotationMarkerFile.delete()) {
            AndroidLog.w(TAG, "Failed to delete rotation marker");
        }
        initLogDirectories();

        if (ShellUtils.checkRootPermission() == 0) {
            ShellUtils.rootExecCmd(
                "for f in /data/adb/lspd/log/modules_*.log /data/adb/lspd/log/verbose_*.log " +
                    "/data/adb/lspd/log.old/modules_*.log /data/adb/lspd/log.old/verbose_*.log; " +
                    "do [ -f \"$f\" ] && : > \"$f\"; done"
            );
        }
    }

    private void ensureDirs(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists() && !dir.mkdirs()) {
                AndroidLog.w(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            }
        }
    }

    private void clearDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else if (!file.delete()) {
                AndroidLog.w(TAG, "Failed to delete file: " + file.getAbsolutePath());
            }
        }
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        clearDirectory(directory);
        if (!directory.delete()) {
            AndroidLog.w(TAG, "Failed to delete directory: " + directory.getAbsolutePath());
        }
    }

    private boolean hasFiles(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    private String buildPageMarker(int pageNum) {
        return PAGE_MARKER_PREFIX + pageNum + PAGE_MARKER_SUFFIX + '\n';
    }

    private long utf8Size(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
