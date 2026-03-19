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
import java.io.File;
import java.io.FileReader;
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

    private static final String LSPD_LOG_DIR = "/data/adb/lspd/log";
    private static final String LSPD_LOG_OLD_DIR = "/data/adb/lspd/log.old";

    private static final String APP_LOG_BASE_DIR = "log";
    private static final String LSPD_COPY_DIR = "lspd";
    private static final String LSPD_LOG_SUBDIR = "log";
    private static final String LSPD_LOG_OLD_SUBDIR = "log.old";
    private static final String FILTERED_MODULE = "Xposed";
    private static final String MODULES_LOG_PREFIX = "modules_";
    private static final String VERBOSE_LOG_PREFIX = "verbose_";

    private static final String HYPERCEILER_TAG = "HyperCeiler";
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

    private XposedLogLoader(Context context) {
        mContext = context.getApplicationContext();
        mAppLogBaseDir = new File(mContext.getFilesDir(), APP_LOG_BASE_DIR);
        mLspdCopyBaseDir = new File(mAppLogBaseDir, LSPD_COPY_DIR);
        mLspdLogDir = new File(mLspdCopyBaseDir, LSPD_LOG_SUBDIR);
        mLspdLogOldDir = new File(mLspdCopyBaseDir, LSPD_LOG_OLD_SUBDIR);
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

        initLogDirectories();
        copyLspdLogs();
        persistFilteredLogsToDatabase();
    }

    private void initLogDirectories() {
        cleanupLegacyRotationMarker();
        ensureDirs(
            mAppLogBaseDir,
            mLspdCopyBaseDir,
            mLspdLogDir,
            mLspdLogOldDir
        );
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

    private void persistFilteredLogsToDatabase() {
        LogDao dao = LogRepository.getInstance().getDao();
        dao.deleteByModule(FILTERED_MODULE);

        List<LogEntry> batch = new ArrayList<>(INSERT_BATCH_SIZE);
        importDirectoryToDatabase(mLspdLogOldDir, LogEntry.SOURCE_GROUP_OLD, dao, batch);
        importDirectoryToDatabase(mLspdLogDir, LogEntry.SOURCE_GROUP_CURRENT, dao, batch);
        if (!batch.isEmpty()) {
            dao.insertAll(batch);
            batch.clear();
        }
        dao.autoTrim();
    }

    private void importDirectoryToDatabase(File directory, String sourceGroup, LogDao dao, List<LogEntry> batch) {
        if (!directory.exists()) {
            return;
        }

        File[] files = getSourceLogFiles(directory);
        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            try {
                importSourceFile(file, sourceGroup, dao, batch);
            } catch (IOException e) {
                AndroidLog.e(TAG, "Failed to import filtered log: " + file.getName(), e);
            }
        }
    }

    private File[] getSourceLogFiles(File directory) {
        File[] files = directory.listFiles((dir, name) ->
            name.startsWith(MODULES_LOG_PREFIX) && name.endsWith(".log")
        );
        if (files == null || files.length == 0) {
            files = directory.listFiles((dir, name) ->
                name.startsWith(VERBOSE_LOG_PREFIX) && name.endsWith(".log")
            );
        }
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
        }
        return files;
    }

    private void importSourceFile(File file, String sourceGroup, LogDao dao, List<LogEntry> batch) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            boolean isTarget = false;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (isTarget && !currentEntry.isEmpty()) {
                        addFilteredEntry(dao, batch, currentEntry.toString(), sourceGroup);
                    }
                    isTarget = line.contains(HYPERCEILER_TAG);
                    currentEntry = new StringBuilder(line);
                } else if (!currentEntry.isEmpty()
                    && currentEntry.length() + line.length() + 1 <= MAX_ENTRY_MESSAGE_LENGTH) {
                    currentEntry.append('\n').append(line);
                }
            }

            if (isTarget && !currentEntry.isEmpty()) {
                addFilteredEntry(dao, batch, currentEntry.toString(), sourceGroup);
            }
        }
    }

    private void addFilteredEntry(LogDao dao, List<LogEntry> batch, String rawEntry, String sourceGroup) {
        LogEntry entry = parseFilteredEntry(rawEntry, sourceGroup);
        if (entry == null) {
            return;
        }
        addBatchEntry(dao, batch, entry);
    }

    private void addBatchEntry(LogDao dao, List<LogEntry> batch, LogEntry entry) {
        batch.add(entry);
        if (batch.size() >= INSERT_BATCH_SIZE) {
            dao.insertAll(batch);
            batch.clear();
        }
    }

    private LogEntry parseFilteredEntry(String rawEntry, String sourceGroup) {
        if (rawEntry == null || rawEntry.isEmpty()) {
            return null;
        }

        int lineBreak = rawEntry.indexOf('\n');
        if (lineBreak < 0) {
            return parseXposedLogLine(rawEntry, sourceGroup);
        }

        String firstLine = rawEntry.substring(0, lineBreak);
        LogEntry template = parseXposedLogLine(firstLine, sourceGroup);
        return buildLogEntry(template, template.getMessage() + '\n' + rawEntry.substring(lineBreak + 1), sourceGroup);
    }

    private LogEntry parseXposedLogLine(String line, String sourceGroup) {
        long timestamp = parseTimestamp(line);
        String level = parseLevel(line);
        String message = extractMessage(line);
        String uidPid = extractUidPid(line);
        String processIds = extractProcessIds(line);
        String tag = extractPackageName(message);

        if (message.contains("[CrashMonitor]")) {
            level = "C";
        }
        return new LogEntry(
            FILTERED_MODULE,
            level,
            tag,
            LogDisplayHelper.withUidPidMeta(message, uidPid),
            timestamp,
            sourceGroup,
            processIds
        );
    }

    private LogEntry buildLogEntry(LogEntry template, String message, String sourceGroup) {
        return new LogEntry(
            FILTERED_MODULE,
            template.getLevel(),
            template.getTag(),
            message.trim(),
            template.getTimestamp(),
            sourceGroup,
            template.getProcessIds()
        );
    }

    private boolean isNewLogEntry(String line) {
        return line != null && line.startsWith("[") && TIME_PATTERN.matcher(line).find();
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

    private String extractProcessIds(String line) {
        Matcher prefixMatcher = LINE_PREFIX_LEVEL_PATTERN.matcher(line);
        if (!prefixMatcher.find()) {
            return "";
        }
        String uid = prefixMatcher.group(1);
        String pid = prefixMatcher.group(2);
        String tid = prefixMatcher.group(3);
        if (uid == null || pid == null || tid == null || uid.isEmpty() || pid.isEmpty() || tid.isEmpty()) {
            return "";
        }
        return uid + ":" + pid + ":" + tid;
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

    private void clearLogs() {
        LogRepository.getInstance().getDao().deleteByModule(FILTERED_MODULE);
        deleteDirectory(mLspdCopyBaseDir);
        cleanupLegacyRotationMarker();
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

    private void cleanupLegacyRotationMarker() {
        File markerFile = new File(mAppLogBaseDir, ".rotation_marker");
        if (markerFile.exists() && !markerFile.delete()) {
            AndroidLog.w(TAG, "Failed to delete legacy rotation marker");
        }
    }
}
