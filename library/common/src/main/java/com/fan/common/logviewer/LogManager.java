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

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 日志管理器 - 单例模式
 */
public class LogManager {
    private static LogManager sInstance;
    private final List<LogEntry> mLogEntries = new ArrayList<>();
    private final List<LogEntry> mXposedLogEntries = new ArrayList<>();
    private Context mContext;
    private boolean mInitialized = false;
    private static final String LOG_BASE_DIR = "log";
    private static final String APP_LOG_DIR = "log/app";
    private static final String APP_LOG_OLD_DIR = "log/app.old";
    private static final String LOG_FILE = "app_logs.txt";
    private static final String DEVICE_INFO_FILE = "devices.txt";
    private static final String TAG = "LogManager";
    private static final int BUFFER_SIZE = 4096;

    private File mLogBaseDir;
    private File mAppLogDir;
    private File mAppLogOldDir;
    private File mAppLogFile;

    // 设备信息提供者
    private static DeviceInfoProvider sDeviceInfoProvider;

    private LogManager() {}

    /**
     * 设置设备信息提供者（在主模块中调用）
     */
    public static void setDeviceInfoProvider(DeviceInfoProvider provider) {
        sDeviceInfoProvider = provider;
    }

    public static void init(Context context) {
        if (sInstance == null) {
            synchronized (LogManager.class) {
                if (sInstance == null) {
                    sInstance = new LogManager();
                    sInstance.doInit(context.getApplicationContext());
                }
            }
        }
    }

    public static LogManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(
                "LogManager not initialized. Call LogManager.init(context) first.");
        }
        return sInstance;
    }

    private void doInit(Context context) {
        this.mContext = context;
        this.mInitialized = true;

        initLogFile();
        loadHistoryLogs();

        AndroidLog.setLogListener((level, tag, message) -> {
            try {
                LogEntry entry = new LogEntry(level, "App", "[" + tag + "] " + message, tag, true);
                addLog(entry);
            } catch (Throwable ignored) {
            }
        });

        addLog(new LogEntry("I", "LogManager", "LogManager initialized", "System", true));

        // 启动时更新设备信息
        updateDeviceInfo();

        // 启动时同步 Xposed 日志
        startXposedLogSync();
    }

    private void startXposedLogSync() {
        new Thread(() -> {
            try {
                XposedLogLoader.loadLogsSync();
                Log.i(TAG, "Xposed log sync completed on startup");
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync Xposed logs on startup", e);
            }
        }).start();
    }

    private void initLogFile() {
        mLogBaseDir = new File(mContext.getFilesDir(), LOG_BASE_DIR);
        mAppLogDir = new File(mContext.getFilesDir(), APP_LOG_DIR);
        mAppLogOldDir = new File(mContext.getFilesDir(), APP_LOG_OLD_DIR);

        if (!mLogBaseDir.exists()) {
            mLogBaseDir.mkdirs();
        }
        if (!mAppLogDir.exists()) {
            mAppLogDir.mkdirs();
        }
        mAppLogFile = new File(mAppLogDir, LOG_FILE);
    }

    // ===== 设备信息相关 =====
    /**
     * 更新设备信息（异步）
     */
    private void updateDeviceInfo() {
        new Thread(() -> {
            try {
                updateDeviceInfoSync();
            } catch (Exception e) {
                Log.e(TAG, "Failed to update device info", e);
            }
        }).start();
    }

    /**
     * 更新设备信息（同步）
     */
    public void updateDeviceInfoSync() {
        if (sDeviceInfoProvider == null) {
            Log.w(TAG, "DeviceInfoProvider not set, skipping device info update");
            return;
        }

        try {
            String deviceInfo = sDeviceInfoProvider.getDeviceInfo(mContext);
            if (deviceInfo != null && !deviceInfo.isEmpty()) {
                saveDeviceInfo(deviceInfo);
                Log.i(TAG, "Device info updated successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update device info", e);
        }
    }

    /**
     * 保存设备信息到文件
     */
    private void saveDeviceInfo(String deviceInfo) {
        try {
            if (!mLogBaseDir.exists()) {
                mLogBaseDir.mkdirs();
            }

            File deviceFile = new File(mLogBaseDir, DEVICE_INFO_FILE);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(deviceFile))) {
                writer.write(deviceInfo);
            }
            Log.d(TAG, "Device info saved to: " + deviceFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save device info", e);
        }
    }

    // ===== 日志轮转 =====

    /**
     * 轮转应用日志（跟随 Xposed 日志轮转）
     */
    public void rotateAppLogs() {
        try {
            if (mAppLogOldDir.exists()) {
                deleteDirectory(mAppLogOldDir);
            }

            if (mAppLogDir.exists() && hasFiles(mAppLogDir)) {
                mAppLogDir.renameTo(mAppLogOldDir);
            }

            mAppLogDir.mkdirs();
            mAppLogFile = new File(mAppLogDir, LOG_FILE);

            synchronized (mLogEntries) {
                mLogEntries.clear();
            }

            Log.i(TAG, "App logs rotated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate app logs", e);
        }
    }

    private boolean hasFiles(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    // ===== 日志操作 =====

    public void addLog(LogEntry entry) {
        if (!mInitialized) return;
        synchronized (mLogEntries) {
            mLogEntries.add(entry);
        }
        saveLogAsync(entry);
    }

    public void addLogs(List<LogEntry> entries) {
        if (!mInitialized) return;
        synchronized (mLogEntries) {
            mLogEntries.addAll(entries);
        }
        saveLogsAsync(entries);
    }

    public void addXposedLog(LogEntry entry) {
        synchronized (mXposedLogEntries) {
            mXposedLogEntries.add(entry);
        }
    }

    public void addXposedLogs(List<LogEntry> entries) {
        synchronized (mXposedLogEntries) {
            mXposedLogEntries.addAll(entries);
        }
    }

    public void clearLogs() {
        synchronized (mLogEntries) {
            mLogEntries.clear();
        }
        deleteLogFile();
        if (mAppLogOldDir.exists()) {
            deleteDirectory(mAppLogOldDir);
        }
    }

    public void clearXposedLogs() {
        synchronized (mXposedLogEntries) {
            mXposedLogEntries.clear();
        }
    }

    /**
     * 清空所有日志（App + Xposed）
     */
    public void clearAllLogs() {
        clearLogs();
        try {
            XposedLogLoader.clearLogsStatic(mContext);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear Xposed logs", e);
        }
    }

    public List<LogEntry> getLogEntries() {
        synchronized (mLogEntries) {
            return new ArrayList<>(mLogEntries);
        }
    }

    public List<LogEntry> getXposedLogEntries() {
        synchronized (mXposedLogEntries) {
            return new ArrayList<>(mXposedLogEntries);
        }
    }

    // ===== 日志持久化 =====

    private void saveLogAsync(LogEntry entry) {
        new Thread(() -> {
            try {
                synchronized (LogManager.class) {
                    FileOutputStream fos = new FileOutputStream(mAppLogFile, true);
                    String line = formatLogLine(entry);
                    fos.write(line.getBytes());
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Save log failed", e);
            }
        }).start();
    }

    private void saveLogsAsync(List<LogEntry> entries) {
        new Thread(() -> {
            try {
                synchronized (LogManager.class) {
                    FileOutputStream fos = new FileOutputStream(mAppLogFile, true);
                    for (LogEntry entry : entries) {
                        String line = formatLogLine(entry);
                        fos.write(line.getBytes());
                    }
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Save logs failed", e);
            }
        }).start();
    }

    private void loadHistoryLogs() {
        File oldLogFile = new File(mAppLogOldDir, LOG_FILE);
        if (oldLogFile.exists()) {
            loadLogsFromFile(oldLogFile);
        }
        loadLogsFromFile(mAppLogFile);
    }

    private void loadLogsFromFile(File file) {
        try {
            if (!file.exists()) return;

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = parseLogLine(line);
                if (entry != null) {
                    synchronized (mLogEntries) {
                        mLogEntries.add(entry);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Load logs from file failed: " + file.getName(), e);
        }
    }

    private void deleteLogFile() {
        if (mAppLogFile != null && mAppLogFile.exists()) {
            mAppLogFile.delete();
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatLogLine(LogEntry entry) {
        String escapedMessage = entry.getMessage().replace("|", "\\|");
        return String.format("%d|%s|%s|%s|%s|%b\n",
            entry.getTimestamp(),
            entry.getLevel(),
            entry.getModule(),
            escapedMessage,
            entry.getTag(),
            entry.isNewLine());
    }

    private LogEntry parseLogLine(String line) {
        try {
            int firstPipe = line.indexOf('|');
            if (firstPipe == -1) return null;

            long timestamp = Long.parseLong(line.substring(0, firstPipe));

            int secondPipe = line.indexOf('|', firstPipe + 1);
            if (secondPipe == -1) return null;
            String level = line.substring(firstPipe + 1, secondPipe);

            int thirdPipe = line.indexOf('|', secondPipe + 1);
            if (thirdPipe == -1) return null;
            String module = line.substring(secondPipe + 1, thirdPipe);

            int lastPipe = line.lastIndexOf('|');
            if (lastPipe == -1 || lastPipe == thirdPipe) return null;
            boolean newLine = Boolean.parseBoolean(line.substring(lastPipe + 1));

            int secondLastPipe = line.lastIndexOf('|', lastPipe - 1);
            if (secondLastPipe == -1 || secondLastPipe == thirdPipe) return null;
            String tag = line.substring(secondLastPipe + 1, lastPipe);

            String message = line.substring(thirdPipe + 1, secondLastPipe).replace("\\|", "|");

            return new LogEntry(timestamp, level, module, message, tag, newLine);
        } catch (Exception e) {
            Log.e(TAG, "Parse log line failed: " + line, e);
            return null;
        }
    }

    // ===== 压缩导出 =====

    /**
     * 创建日志压缩包到缓存目录
     * @return 压缩文件，失败返回 null
     */
    public File createLogZipFile() {
        try {
            // 导出前先更新设备信息
            updateDeviceInfoSync();

            File cacheDir = new File(mContext.getCacheDir(), "log_export");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            cleanOldExportFiles(cacheDir);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String zipFileName = "hyperceiler_logs_" + timestamp + ".zip";
            File zipFile = new File(cacheDir, zipFileName);

            if (createZipFromDirectory(mLogBaseDir, zipFile)) {
                Log.i(TAG, "Log zip created: " + zipFile.getAbsolutePath());
                return zipFile;
            } else {
                Log.e(TAG, "Failed to create log zip");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create log zip file", e);
            return null;
        }
    }

    /**
     * 导出日志压缩包到指定 Uri
     */
    public boolean exportLogsZipToUri(Uri uri) {
        File zipFile = null;
        try {
            zipFile = createLogZipFile();
            if (zipFile == null || !zipFile.exists()) {
                return false;
            }

            OutputStream os = mContext.getContentResolver().openOutputStream(uri);
            if (os == null) {
                return false;
            }

            FileInputStream fis = new FileInputStream(zipFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            fis.close();
            os.flush();
            os.close();

            Log.i(TAG, "Logs exported to Uri successfully");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Export to Uri failed", e);
            return false;
        } finally {
            if (zipFile != null && zipFile.exists()) {
                zipFile.delete();
            }
        }
    }

    /**
     * 创建压缩文件
     */
    private boolean createZipFromDirectory(File sourceDir, File zipFile) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            Log.e(TAG, "Source directory does not exist: " + sourceDir.getAbsolutePath());
            return false;
        }

        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            addDirectoryToZip(zos, sourceDir, sourceDir.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create zip file", e);
            return false;
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 递归添加目录到压缩文件
     */
    private void addDirectoryToZip(ZipOutputStream zos, File directory, String basePath) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryPath = basePath + "/" + file.getName();

            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(entryPath + "/"));
                zos.closeEntry();
                addDirectoryToZip(zos, file, entryPath);
            } else {
                addFileToZip(zos, file, entryPath);
            }
        }
    }

    /**
     * 添加单个文件到压缩文件
     */
    private void addFileToZip(ZipOutputStream zos, File file, String entryPath) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(entryPath);
            entry.setTime(file.lastModified());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 清理旧的导出文件（保留最近 5 个）
     */
    private void cleanOldExportFiles(File exportDir) {
        File[] files = exportDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null || files.length <= 5) return;

        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (int i = 5; i < files.length; i++) {
            files[i].delete();
        }
    }

    /**
     * 生成默认的压缩文件名
     */
    public static String generateZipFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "hyperceiler_logs_" + sdf.format(new Date()) + ".zip";
    }

    /**
     * 获取单条日志的格式化文本（用于复制）
     */
    public static String formatLogEntryForCopy(LogEntry entry) {
        return String.format("[%s] %s/%s [%s]:\n%s",
            entry.getFormattedTime(),
            entry.getLevel(),
            entry.getTag(),
            entry.getModule(),
            entry.getMessage());
    }

    /**
     * 获取 Context
     */
    public Context getContext() {
        return mContext;
    }
}
