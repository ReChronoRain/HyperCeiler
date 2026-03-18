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

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.home.safemode.AppCrashStore;
import com.sevtinge.hyperceiler.home.safemode.CrashRecordStore;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogManager {

    private static final String TAG = "LogManager";
    private static final String APP_MODULE = "App";
    private static final String APP_EXPORT_RELATIVE_PATH = "app/app_runtime.log";

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

                // 构造入库对象 (Module 为 "App")
                LogEntry entry = new LogEntry("App", finalLevel, tag, message);

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
        File exportDir = new File(mAppContext.getCacheDir(), "log_export");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return null;
        }

        File sourceDir = new File(mAppContext.getFilesDir(), "log");
        if (!sourceDir.exists() && !sourceDir.mkdirs()) {
            return null;
        }

        File zipFile = new File(exportDir, generateZipFileName());

        try {
            if (!syncAppLogsForExport(sourceDir)) {
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

    private boolean syncAppLogsForExport(File logRootDir) {
        File appLogFile = new File(logRootDir, APP_EXPORT_RELATIVE_PATH);
        File appLogParent = appLogFile.getParentFile();
        if (appLogParent != null && !appLogParent.exists() && !appLogParent.mkdirs()) {
            AndroidLog.e(TAG, "Export logs: failed to create app log export directory");
            return false;
        }

        List<LogEntry> appLogs = LogRepository.getInstance().getLogsByModuleForExportSync(APP_MODULE);
        if (appLogs.isEmpty()) {
            if (appLogFile.exists() && !appLogFile.delete()) {
                AndroidLog.w(TAG, "Export logs: failed to delete stale app log export file");
            }
            return true;
        }

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(appLogFile, false), StandardCharsets.UTF_8))) {
            for (LogEntry entry : appLogs) {
                writer.write(formatLogEntryForCopy(entry));
                writer.newLine();
                writer.newLine();
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Export logs: failed to sync app logs before export", e);
            return false;
        }
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
}
