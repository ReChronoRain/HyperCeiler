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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.home.safemode.AppCrashStore;
import com.sevtinge.hyperceiler.home.safemode.CrashRecordStore;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogManager {

    private static final String TAG = "LogManager";
    private static final String APP_MODULE = "App";
    private static final String FILTERED_MODULE = "Xposed";
    private static final String EXPORT_ROOT_DIR = "log";
    private static final String EXPORT_CACHE_DIR = "log_export";
    private static final String APP_EXPORT_DIR = "app";
    private static final String FILTERED_EXPORT_DIR = "hyperceiler_filtered";
    private static final String CURRENT_LOG_DIR = "log";
    private static final String OLD_LOG_DIR = "log.old";
    private static final String LSPD_EXPORT_DIR = "lspd";
    private static final String APP_EXPORT_FILE_NAME = "app_runtime.log";
    private static final String FILTERED_EXPORT_FILE_NAME = "hyperceiler.log";
    private static final String DEVICES_FILE_NAME = "devices.txt";
    private static final String SCOPES_FILE_NAME = "scopes.txt";
    private static final String LOG_CONFIG_FILE_NAME = "log_config_v2";
    private static final String APP_LOGGER_NAME = "HyperCeilerApp";
    private static final String APP_EXPORT_MODULE_PREFIX = "[com.sevtinge.hyperceiler,App] ";
    private static final String FILTERED_EXPORT_MODULE_PREFIX = "[com.sevtinge.hyperceiler,HyperCeiler] ";
    private static final String PAGE_MARKER_PREFIX = "----part ";
    private static final String PAGE_MARKER_SUFFIX = " start----";
    private static final long EXPORT_PAGE_SIZE_BYTES = 4L * 1024 * 1024;
    private static final int EXPORT_QUERY_BATCH_SIZE = 500;

    private static volatile LogManager sInstance;
    private static volatile boolean sStatusManagerInitialized;

    private Context mAppContext;
    private static DeviceInfoProvider sDeviceInfoProvider;

    public static LogManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("LogManager must be initialized in Application!");
        }
        return sInstance;
    }

    public static void init(Context context) {
        init(context, null, null);
    }

    public static void init(Context context, @Nullable DeviceInfoProvider provider, @Nullable Runnable xposedLogSyncer) {
        if (sInstance == null) {
            synchronized (LogManager.class) {
                if (sInstance == null) sInstance = new LogManager(context);
            }
        }
        if (provider != null) {
            setDeviceInfoProvider(provider);
        }
        if (!sStatusManagerInitialized) {
            synchronized (LogManager.class) {
                if (!sStatusManagerInitialized) {
                    LogStatusManager.init(
                        context,
                        context.getDataDir().getAbsolutePath(),
                        xposedLogSyncer,
                        null
                    );
                    sStatusManagerInitialized = true;
                }
            }
        }
    }

    private LogManager(Context context) {
        mAppContext = context.getApplicationContext();
        // 1. 确保底层的 Repository 先初始化
        LogRepository.init(context);

        // 2. 开启日志拦截：捕获 App 自身的 AndroidLog 输出
        initLogInterceptor();
    }


    /**
     * 设置设备信息提供者（在主模块中调用）
     */
    public static void setDeviceInfoProvider(DeviceInfoProvider provider) {
        sDeviceInfoProvider = provider;
    }

    /**
     * 核心逻辑：拦截 App 内部 Log 输出并存入数据库
     */
    private void initLogInterceptor() {
        AndroidLog.setLogListener((level, tag, message) -> {
            try {
                // 如果是 Crash 标签，级别标记为 C
                String finalLevel = "Crash".equals(tag) ? "C" : level;
                String processIds = Process.myUid() + ":" + Process.myPid() + ":" + Process.myTid();

                // 构造入库对象 (Module 为 "App")
                LogEntry entry = new LogEntry(
                    APP_MODULE,
                    finalLevel,
                    tag,
                    message,
                    System.currentTimeMillis(),
                    LogEntry.SOURCE_GROUP_CURRENT,
                    processIds
                );

                // 丢给 Repository 执行异步插入
                LogRepository.getInstance().insertLog(entry);
            } catch (Exception ignored) {
            }
        });

        // 添加一条启动日志
        // addLog(new LogEntry("App", "I", "LogManager", "LogManager 初始化成功"));
    }

    /**
     * 手动添加日志
     */
    public void addLog(LogEntry entry) {
        LogRepository.getInstance().insertLog(entry);
    }

    /**
     * 查询日志 (供 Fragment 使用)
     */
    public List<LogEntry> query(String module, String level, String tag, String keyword) {
        return LogRepository.getInstance().getDao().queryLogs(
            module,
            (level == null || LogLevelFilter.isAll(level)) ? LogLevelFilter.ALL.getValue() : level,
            tag == null ? "" : tag,
            keyword == null ? "" : keyword
        );
    }

    /**
     * 清空所有日志
     */
    public void clearAllLogs() {
        LogRepository.getInstance().getDao().clearAll();
        XposedLogLoader.clearAllSync(mAppContext);
        CrashRecordStore.clearAll(mAppContext);
        AppCrashStore.clear(mAppContext);
    }

    /**
     * 获取单条日志格式化文本（用于复制）
     */
    public static String formatLogEntryForCopy(LogEntry entry) {
        return String.format("[%s] %s/%s [%s]:\n%s",
            entry.getFormattedTime(),
            entry.getLevel(),
            entry.getTag(),
            entry.getModule(),
            LogDisplayHelper.getExportMessage(entry.getModule(), entry.getMessage()));
    }

    public static String generateZipFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return "hyperceiler_logs_" + sdf.format(new Date()) + ".zip";
    }

    /**
     * 创建日志压缩包到缓存目录
     */
    public File createLogZipFile() {
        File exportDir = new File(mAppContext.getCacheDir(), EXPORT_CACHE_DIR);
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return null;
        }

        File sourceDir = new File(exportDir, EXPORT_ROOT_DIR);
        if (!prepareCleanDirectory(sourceDir)) {
            return null;
        }

        File zipFile = new File(exportDir, generateZipFileName());

        try {
            File lspdSourceDir = new File(new File(mAppContext.getFilesDir(), EXPORT_ROOT_DIR), LSPD_EXPORT_DIR);
            if (!copyDirectoryForExport(lspdSourceDir, new File(sourceDir, LSPD_EXPORT_DIR))) {
                return null;
            }
            if (!prepareExportMetaFiles(sourceDir)) {
                return null;
            }
            if (!syncDatabaseLogsForExport(sourceDir)) {
                return null;
            }
            if (zipDirectory(sourceDir, zipFile)) return zipFile;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Export logs: failed to create zip", e);
        }
        if (zipFile.exists()) zipFile.delete();
        return null;
    }

    /**
     * 导出日志压缩包到指定 Uri
     */
    public boolean exportLogsZipToUri(Uri uri) {
        File zipFile = createLogZipFile();
        if (zipFile == null || !zipFile.exists()) return false;

        try (InputStream is = new FileInputStream(zipFile);
             OutputStream os = mAppContext.getContentResolver().openOutputStream(uri)) {
            if (os == null) return false;
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return true;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Export logs: failed to write zip to uri", e);
            return false;
        } finally {
            if (zipFile.exists()) zipFile.delete();
        }
    }

    private boolean zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addPathToZip(sourceDir, sourceDir.getName(), zos);
            return true;
        }
    }

    private boolean syncDatabaseLogsForExport(File logRootDir) {
        return exportAppLogs(new File(logRootDir, APP_EXPORT_DIR))
            && exportFilteredLogs(new File(logRootDir, FILTERED_EXPORT_DIR));
    }

    private boolean prepareExportMetaFiles(File logRootDir) {
        return writeDevicesFile(logRootDir)
            && writeScopesFile(logRootDir)
            && copyOptionalFileForExport(
            new File(new File(mAppContext.getFilesDir(), EXPORT_ROOT_DIR), LOG_CONFIG_FILE_NAME),
            new File(logRootDir, LOG_CONFIG_FILE_NAME)
        );
    }

    private boolean writeDevicesFile(File logRootDir) {
        if (sDeviceInfoProvider == null) {
            return true;
        }
        File devicesFile = new File(logRootDir, DEVICES_FILE_NAME);
        File parent = devicesFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            AndroidLog.e(TAG, "Export logs: failed to create devices file parent");
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(devicesFile, false), StandardCharsets.UTF_8))) {
            String content = sDeviceInfoProvider.getDeviceInfo(mAppContext);
            writer.write(content == null ? "" : content);
            writer.flush();
            return true;
        } catch (Exception e) {
            AndroidLog.e(TAG, "Export logs: failed to write devices file", e);
            return false;
        }
    }

    private boolean writeScopesFile(File logRootDir) {
        File scopesFile = new File(logRootDir, SCOPES_FILE_NAME);
        File parent = scopesFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            AndroidLog.e(TAG, "Export logs: failed to create scopes file parent");
            return false;
        }

        List<String> scopes = ScopeManager.getScopeSync();
        List<String> exportScopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
        Collections.sort(exportScopes);

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(scopesFile, false), StandardCharsets.UTF_8))) {
            writer.write("# HyperCeiler checked scope list");
            writer.newLine();
            if (scopes == null) {
                writer.write("# unavailable: LSPosed service not connected");
                writer.newLine();
            } else {
                writer.write("count=" + exportScopes.size());
                writer.newLine();
                for (String scope : exportScopes) {
                    writer.write(scope);
                    writer.newLine();
                }
            }
            writer.flush();
            return true;
        } catch (Exception e) {
            AndroidLog.e(TAG, "Export logs: failed to write scopes file", e);
            return false;
        }
    }

    private boolean copyOptionalFileForExport(File source, File target) {
        if (!source.exists()) {
            return true;
        }
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                AndroidLog.e(TAG, "Export logs: failed to create parent for optional file");
                return false;
            }
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Export logs: failed to copy optional file " + source.getAbsolutePath(), e);
            return false;
        }
    }

    private boolean exportAppLogs(File outputDir) {
        return writePagedLogFiles(outputDir, APP_EXPORT_FILE_NAME, APP_MODULE, null);
    }

    private boolean exportFilteredLogs(File outputDir) {
        return writePagedLogFiles(
            new File(outputDir, CURRENT_LOG_DIR),
            FILTERED_EXPORT_FILE_NAME,
            FILTERED_MODULE,
            LogEntry.SOURCE_GROUP_CURRENT
        ) && writePagedLogFiles(
            new File(outputDir, OLD_LOG_DIR),
            FILTERED_EXPORT_FILE_NAME,
            FILTERED_MODULE,
            LogEntry.SOURCE_GROUP_OLD
        );
    }

    private boolean writePagedLogFiles(File outputDir, String baseFileName, String module, @Nullable String sourceGroup) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            AndroidLog.e(TAG, "Export logs: failed to create output directory for " + outputDir.getAbsolutePath());
            return false;
        }

        int offset = 0;
        int pageIndex = 1;
        long pageSize = 0;
        long markerSize = 0;
        boolean hasWrittenContent = false;
        StringBuilder pageBuilder = new StringBuilder();

        String marker = buildPageMarker(pageIndex);
        markerSize = utf8Size(marker);
        pageBuilder.append(marker);
        pageSize = markerSize;

        while (true) {
            List<LogEntry> logs = sourceGroup == null
                ? LogRepository.getInstance().getLogsByModulePageForExportSync(module, EXPORT_QUERY_BATCH_SIZE, offset)
                : LogRepository.getInstance().getLogsByModuleAndSourceGroupPageForExportSync(
                    module,
                    sourceGroup,
                    EXPORT_QUERY_BATCH_SIZE,
                    offset
                );
            if (logs.isEmpty()) {
                break;
            }
            offset += logs.size();

            for (LogEntry entry : logs) {
                String exportText = formatLogEntryForExport(entry);
                if (!exportText.endsWith("\n")) {
                    exportText += '\n';
                }
                long entrySize = utf8Size(exportText);
                if (pageSize > markerSize && pageSize + entrySize > EXPORT_PAGE_SIZE_BYTES) {
                    if (!writePageFile(outputDir, baseFileName, pageIndex, pageBuilder)) {
                        return false;
                    }
                    hasWrittenContent = true;
                    pageIndex++;
                    pageBuilder.setLength(0);
                    marker = buildPageMarker(pageIndex);
                    markerSize = utf8Size(marker);
                    pageBuilder.append(marker);
                    pageSize = markerSize;
                }
                pageBuilder.append(exportText);
                pageSize += entrySize;
            }
        }

        if (pageSize > markerSize) {
            if (!writePageFile(outputDir, baseFileName, pageIndex, pageBuilder)) {
                return false;
            }
            hasWrittenContent = true;
        }

        if (!hasWrittenContent) {
            AndroidLog.d(TAG, "Export logs: no entries for " + module + (sourceGroup == null ? "" : " [" + sourceGroup + "]"));
        }
        return true;
    }

    private boolean writePageFile(File outputDir, String baseFileName, int pageIndex, StringBuilder content) {
        File outputFile = buildPagedOutputFile(outputDir, baseFileName, pageIndex);
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outputFile, false), StandardCharsets.UTF_8))) {
            writer.write(content.toString());
            writer.flush();
            return true;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Export logs: failed to write file " + outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    private File buildPagedOutputFile(File outputDir, String baseFileName, int pageIndex) {
        int extensionIndex = baseFileName.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == baseFileName.length() - 1) {
            return new File(outputDir, baseFileName + ".part" + pageIndex);
        }
        String name = baseFileName.substring(0, extensionIndex);
        String extension = baseFileName.substring(extensionIndex);
        return new File(outputDir, name + ".part" + pageIndex + extension);
    }

    private String formatLogEntryForExport(LogEntry entry) {
        return FILTERED_MODULE.equals(entry.getModule())
            ? formatFilteredLogEntry(entry)
            : formatAppLogEntry(entry);
    }

    private String formatFilteredLogEntry(LogEntry entry) {
        String message = LogDisplayHelper.getExportMessage(entry.getModule(), entry.getMessage());
        return buildStructuredLogLine(entry, "LSPosedFramework", FILTERED_EXPORT_MODULE_PREFIX, message);
    }

    private String formatAppLogEntry(LogEntry entry) {
        String message = entry.getMessage() == null ? "" : entry.getMessage();
        String tag = entry.getTag() == null || entry.getTag().isEmpty() ? LogDisplayHelper.OTHER_TAG_VALUE : entry.getTag();
        return buildStructuredLogLine(entry, APP_LOGGER_NAME, APP_EXPORT_MODULE_PREFIX, "[" + tag + "] " + message);
    }

    private String buildStructuredLogLine(LogEntry entry, String loggerName, String modulePrefix, String message) {
        String[] processParts = splitProcessIds(entry.getProcessIds());
        return String.format(
            Locale.US,
            "[ %s %8s:%6s:%6s %s/%s ] %s%s",
            formatFullTimestamp(entry.getTimestamp()),
            processParts[0],
            processParts[1],
            processParts[2],
            sanitizeLevel(entry.getLevel()),
            loggerName,
            modulePrefix,
            message == null ? "" : message
        );
    }

    private String[] splitProcessIds(String processIds) {
        String[] parts = {"0", "0", "0"};
        if (processIds == null || processIds.isEmpty()) {
            return parts;
        }
        String[] sourceParts = processIds.split(":");
        for (int i = 0; i < parts.length && i < sourceParts.length; i++) {
            if (!sourceParts[i].isEmpty()) {
                parts[i] = sourceParts[i];
            }
        }
        return parts;
    }

    private String sanitizeLevel(String level) {
        if (level == null || level.isEmpty()) {
            return "I";
        }
        return "C".equals(level) ? "E" : level;
    }

    private String formatFullTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(new Date(timestamp));
    }

    private String buildPageMarker(int pageIndex) {
        return PAGE_MARKER_PREFIX + pageIndex + PAGE_MARKER_SUFFIX + '\n';
    }

    private void addPathToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        String normalizedName = entryName.replace(File.separatorChar, '/');
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null || children.length == 0) {
                ZipEntry entry = new ZipEntry(normalizedName + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();
                return;
            }
            for (File child : children) {
                addPathToZip(child, normalizedName + "/" + child.getName(), zos);
            }
            return;
        }

        ZipEntry entry = new ZipEntry(normalizedName);
        zos.putNextEntry(entry);
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    private boolean prepareCleanDirectory(File directory) {
        if (directory.exists() && !clearDirectory(directory)) {
            return false;
        }
        return directory.exists() || directory.mkdirs();
    }

    private boolean clearDirectory(File directory) {
        File[] children = directory.listFiles();
        if (children == null) {
            return true;
        }
        for (File child : children) {
            boolean deleted = child.isDirectory() ? deleteDirectory(child) : child.delete();
            if (!deleted) {
                AndroidLog.w(TAG, "Export logs: failed to delete " + child.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private boolean deleteDirectory(File directory) {
        return clearDirectory(directory) && directory.delete();
    }

    private boolean copyDirectoryForExport(File source, File target) throws IOException {
        if (!source.exists()) {
            return true;
        }
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                AndroidLog.e(TAG, "Export logs: failed to create directory " + target.getAbsolutePath());
                return false;
            }
            File[] children = source.listFiles();
            if (children == null) {
                return true;
            }
            for (File child : children) {
                if (!copyDirectoryForExport(child, new File(target, child.getName()))) {
                    return false;
                }
            }
            return true;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            AndroidLog.e(TAG, "Export logs: failed to create file parent " + parent.getAbsolutePath());
            return false;
        }
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private long utf8Size(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
