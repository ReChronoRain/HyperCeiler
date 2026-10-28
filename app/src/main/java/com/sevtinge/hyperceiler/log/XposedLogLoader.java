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
    private static final String PROC_ROOT_PREFIX = "/proc/1/root";
    private static final int LSPD_SOURCE_UNAVAILABLE = 20;
    private static final int LSPD_EMPTY = 21;
    private static final int LSPD_STAGE_FAILED = 22;
    private static final int LSPD_COPY_FAILED = 24;
    private static final int LSPD_CHMOD_FAILED = 25;
    private static final int LSPD_REPLACE_FAILED = 26;

    private static final String APP_LOG_BASE_DIR = "log";
    private static final String LSPD_COPY_DIR = "lspd";
    private static final String LSPD_LOG_SUBDIR = "log";
    private static final String LSPD_LOG_OLD_SUBDIR = "log.old";
    private static final String FILTERED_MODULE = "Xposed";
    private static final String MODULES_LOG_PREFIX = "modules";
    private static final String VERBOSE_LOG_PREFIX = "verbose";

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
    private static volatile boolean sSyncDeferredUntilNextLaunch;
    private volatile LspdSourceAccess mPreferredSourceAccess;

    private static String buildLspdCopyCommand(LspdSourceAccess access, String sourceDir, File targetDir) {
        String script = buildLspdCopyScript(sourceDir, targetDir.getAbsolutePath());
        return switch (access) {
            case DIRECT, PROC_ROOT -> script;
            case NSENTER_MOUNT -> "nsenter --mount=/proc/1/ns/mnt -- /system/bin/sh -c "
                + shellQuote(script);
            case NSENTER_TARGET -> "nsenter -t 1 -m /system/bin/sh -c " + shellQuote(script);
        };
    }

    private final Context mContext;
    private final File mAppLogBaseDir;
    private final File mLspdCopyBaseDir;
    private final File mLspdLogDir;
    private final File mLspdLogOldDir;

    static String buildLspdCopyScript(String sourceDir, String targetDir) {
        return "SOURCE=" + shellQuote(sourceDir) + "\n"
            + "TARGET=" + shellQuote(targetDir) + "\n"
            + "STAGE=\"${TARGET}.hc-copy-$$\"\n"
            + "if [ ! -d \"$SOURCE\" ]; then echo HC_LSPD_SOURCE_UNAVAILABLE; exit "
            + LSPD_SOURCE_UNAVAILABLE + "; fi\n"
            + "rm -rf \"$STAGE\"\n"
            + "mkdir -p \"$TARGET\" \"$STAGE\" || { rm -rf \"$STAGE\"; echo HC_LSPD_STAGE_FAILED; exit "
            + LSPD_STAGE_FAILED + "; }\n"
            + "found=0\n"
            + "for file in \"$SOURCE\"/modules*.log \"$SOURCE\"/verbose*.log; do\n"
            + "  [ -f \"$file\" ] || continue\n"
            + "  cp -f \"$file\" \"$STAGE/\" || { rm -rf \"$STAGE\"; echo HC_LSPD_COPY_FAILED; exit "
            + LSPD_COPY_FAILED + "; }\n"
            + "  found=1\n"
            + "done\n"
            + "if [ \"$found\" -eq 0 ]; then\n"
            + "  rm -rf \"$STAGE\"\n"
            + "  echo HC_LSPD_EMPTY\n"
            + "  exit " + LSPD_EMPTY + "\n"
            + "fi\n"
            + "chmod 644 \"$STAGE\"/* || { rm -rf \"$STAGE\"; echo HC_LSPD_CHMOD_FAILED; exit "
            + LSPD_CHMOD_FAILED + "; }\n"
            + "BACKUP=\"${TARGET}.hc-backup-$$\"\n"
            + "rm -rf \"$BACKUP\"\n"
            + "if [ -d \"$TARGET\" ]; then mv \"$TARGET\" \"$BACKUP\" || { rm -rf \"$STAGE\"; "
            + "echo HC_LSPD_REPLACE_FAILED; exit "
            + LSPD_REPLACE_FAILED + "; }; fi\n"
            + "mkdir -p \"$TARGET\" || { [ -d \"$BACKUP\" ] && mv \"$BACKUP\" \"$TARGET\"; "
            + "rm -rf \"$STAGE\"; echo HC_LSPD_REPLACE_FAILED; exit " + LSPD_REPLACE_FAILED + "; }\n"
            + "for file in \"$STAGE\"/*; do\n"
            + "  [ -f \"$file\" ] || continue\n"
            + "  mv -f \"$file\" \"$TARGET/\" || { rm -rf \"$TARGET\"; "
            + "[ -d \"$BACKUP\" ] && mv \"$BACKUP\" \"$TARGET\"; "
            + "rm -rf \"$STAGE\"; echo HC_LSPD_REPLACE_FAILED; exit " + LSPD_REPLACE_FAILED + "; }\n"
            + "done\n"
            + "rm -rf \"$BACKUP\" \"$STAGE\"\n"
            + "echo HC_LSPD_COPY_OK\n"
            + "exit 0\n";
    }

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
        LogSyncDiagnostics.context(context, LogRepository.getInstance().getDao());
        if (sSyncDeferredUntilNextLaunch) {
            AndroidLog.i(TAG, "Skip Xposed log sync until next app launch.");
            // LSPosed keeps its old files, but new native logs can still be imported
            // after the persisted clear cutoff without resurrecting cleared records.
            if (LogSyncDiagnostics.checkRoot(LogRepository.getInstance().getDao())) {
                LogcatLogLoader.sync(context, LogRepository.getInstance().getDao());
            }
            return;
        }
        try {
            getInstance(context).syncToDatabase();
        } catch (RuntimeException failure) {
            LogSyncDiagnostics.record(LogRepository.getInstance().getDao(), "LogSync.LSPosed", "E",
                "LSPOSED_SYNC_START_FAILED: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            if (LogSyncDiagnostics.checkRoot(LogRepository.getInstance().getDao()))
                LogcatLogLoader.sync(context, LogRepository.getInstance().getDao());
        }
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
        sSyncDeferredUntilNextLaunch = true;
        LogcatLogLoader.markCleared(context);
        getInstance(context).clearLogs();
    }

    public static void clearAll(Context context) {
        new Thread(() -> clearAllSync(context), "XposedLogClear").start();
    }

    private void syncToDatabase() {
        if (!LogSyncDiagnostics.checkRoot(LogRepository.getInstance().getDao())) return;
        try {
            initLogDirectories();
            if (copyLspdLogs()) persistFilteredLogsToDatabase();
            // A failed copy must not replace the last usable LSPosed records with an empty/partial capture.
        } catch (RuntimeException failure) {
            LogSyncDiagnostics.record(LogRepository.getInstance().getDao(), "LogSync.LSPosed", "E",
                "LSPOSED_IMPORT_FAILED: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        } finally {
            LogcatLogLoader.sync(mContext, LogRepository.getInstance().getDao());
        }
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

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String commandDetail(ShellUtils.CommandResult result) {
        StringBuilder detail = new StringBuilder();
        if (result.successMsg != null) {
            detail.append(result.successMsg);
        }
        if (result.errorMsg != null) {
            if (detail.length() > 0) {
                detail.append(' ');
            }
            detail.append(result.errorMsg);
        }
        String text = detail.toString().replace('\r', ' ').replace('\n', ' ').trim();
        return text.length() <= 2048 ? text : text.substring(0, 2048);
    }

    private static String detailSuffix(String detail) {
        return detail.isEmpty() ? "" : " detail=" + detail;
    }

    private boolean copyLspdLogs() {
        final LspdCopyStatus current = copyLspdDirectory(LSPD_LOG_DIR, mLspdLogDir);
        // The old log directory is optional. A missing or failed old capture must not
        // prevent the current capture from being persisted.
        copyLspdDirectory(LSPD_LOG_OLD_DIR, mLspdLogOldDir);
        return current == LspdCopyStatus.COPIED || current == LspdCopyStatus.EMPTY;
    }

    private LspdCopyStatus copyLspdDirectory(String sourceDir, File targetDir) {
        final String diagnosticTag = "LogSync.LSPosed." + (LSPD_LOG_DIR.equals(sourceDir) ? "current" : "old");
        final LogDao dao = LogRepository.getInstance().getDao();
        final List<String> attemptDetails = new ArrayList<>();
        boolean sawEmptyDirectory = false;
        boolean sawFailure = false;

        for (LspdSourceAccess access : getSourceAccessOrder()) {
            final String actualSource = access == LspdSourceAccess.PROC_ROOT
                ? PROC_ROOT_PREFIX + sourceDir
                : sourceDir;
            final String command = buildLspdCopyCommand(access, actualSource, targetDir);
            final ShellUtils.CommandResult result = ShellUtils.execRootCommand(command);
            final String detail = commandDetail(result);

            if (result.result == 0) {
                mPreferredSourceAccess = access;
                LogSyncDiagnostics.record(dao, diagnosticTag, "I",
                    "LSPOSED_COPY_OK: " + sourceDir + " via=" + access.id + detailSuffix(detail));
                return LspdCopyStatus.COPIED;
            }
            if (result.result == LSPD_EMPTY) {
                sawEmptyDirectory = true;
                continue;
            }
            if (result.result != LSPD_SOURCE_UNAVAILABLE) {
                sawFailure = true;
            }
            attemptDetails.add(access.id + "=" + result.result + detailSuffix(detail));
        }

        final String attempts = String.join(", ", attemptDetails);
        if (sawFailure) {
            LogSyncDiagnostics.record(dao, diagnosticTag, "E",
                "LSPOSED_COPY_FAILED: " + sourceDir + " attempts=" + attempts);
            return LspdCopyStatus.FAILED;
        }
        if (sawEmptyDirectory) {
            clearDirectory(targetDir);
            LogSyncDiagnostics.record(dao, diagnosticTag, "I",
                "LSPOSED_DIRECTORY_EMPTY: " + sourceDir + " attempts=" + attempts);
            return LspdCopyStatus.EMPTY;
        }
        LogSyncDiagnostics.record(dao, diagnosticTag, LSPD_LOG_DIR.equals(sourceDir) ? "W" : "I",
            "LSPOSED_DIRECTORY_UNAVAILABLE: " + sourceDir + " attempts=" + attempts
                + "; framework log location may differ or logging may be disabled. "
                + "Native logcat collection runs separately.");
        return LspdCopyStatus.UNAVAILABLE;
    }

    private List<LspdSourceAccess> getSourceAccessOrder() {
        List<LspdSourceAccess> accesses = new ArrayList<>(LspdSourceAccess.values().length);
        LspdSourceAccess preferred = mPreferredSourceAccess;
        if (preferred != null) {
            accesses.add(preferred);
        }
        for (LspdSourceAccess access : LspdSourceAccess.values()) {
            if (access != preferred) {
                accesses.add(access);
            }
        }
        return accesses;
    }

    private enum LspdCopyStatus {
        COPIED,
        EMPTY,
        UNAVAILABLE,
        FAILED
    }

    private enum LspdSourceAccess {
        DIRECT("direct"),
        PROC_ROOT("proc_root"),
        NSENTER_MOUNT("nsenter_mount"),
        NSENTER_TARGET("nsenter_target");

        private final String id;

        LspdSourceAccess(String id) {
            this.id = id;
        }
    }

    private void persistFilteredLogsToDatabase() {
        LogDao dao = LogRepository.getInstance().getDao();
        dao.deleteSyncedXposedLogs(FILTERED_MODULE);

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
        if (files == null) {
            return;
        }

        boolean hasModules = Arrays.stream(files).anyMatch(file -> file.getName().startsWith(MODULES_LOG_PREFIX));
        for (File file : files) {
            try {
                // Verbose repeats module records, but also owns pre-module load failures.
                // Always scan those failures even when modules_ logs are available.
                boolean frameworkOnly = hasModules && file.getName().startsWith(VERBOSE_LOG_PREFIX);
                importSourceFile(file, sourceGroup, dao, batch, frameworkOnly);
            } catch (IOException e) {
                AndroidLog.e(TAG, "Failed to import filtered log: " + file.getName(), e);
            }
        }
    }

    private File[] getSourceLogFiles(File directory) {
        File[] files = directory.listFiles((dir, name) ->
            (name.startsWith(MODULES_LOG_PREFIX) || name.startsWith(VERBOSE_LOG_PREFIX)) && name.endsWith(".log")
        );
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
        }
        return files;
    }

    private void importSourceFile(File file, String sourceGroup, LogDao dao, List<LogEntry> batch,
                                  boolean frameworkOnly) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 16384)) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            boolean isTarget = false;

            while ((line = reader.readLine()) != null) {
                if (isNewLogEntry(line)) {
                    if (isTarget && !currentEntry.isEmpty()) {
                        addFilteredEntry(dao, batch, currentEntry.toString(), sourceGroup);
                    }
                    isTarget = (!frameworkOnly && line.contains(HYPERCEILER_TAG)) || LspdLoadFailure.matches(line);
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

        if (LspdLoadFailure.matches(line)) {
            tag = "LSPosed.ModuleLoad";
            message = LspdLoadFailure.explain(message);
        }

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
            if (level != null && !level.isEmpty()) {
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
