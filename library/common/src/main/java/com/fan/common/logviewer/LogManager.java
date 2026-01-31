// LogManager.java
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
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 日志管理器 - 单例模式
 */
public class LogManager {
    private static volatile LogManager sInstance;
    private final List<LogEntry> mLogEntries = new ArrayList<>();
    private final List<LogEntry> mXposedLogEntries = new ArrayList<>();
    private WeakReference<Context> mContextRef;
    private Context mAppContext;
    private boolean mInitialized = false;
    private static final String LOG_BASE_DIR = "log";
    private static final String APP_LOG_BASE_DIR = "log/app";
    private static final String APP_LOG_DIR = "log/app/log";
    private static final String APP_LOG_OLD_DIR = "log/app/log.old";
    private static final String LOG_FILE_PREFIX = "app_";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final String DEVICE_INFO_FILE = "devices.txt";
    private static final String TAG = "LogManager";
    private static final int BUFFER_SIZE = 4096;

    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
        "^\\[\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+" +
            "(\\d+):\\s*(\\d+):\\s*(\\d+)\\s+" +
                "([VDIWEF])/([^\\s\\]]+)\\s*]\\s*(.*)$"
    );
    private static final SimpleDateFormat LOG_TIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    private File mLogBaseDir;
    private File mAppLogBaseDir;
    private File mAppLogDir;
    private File mAppLogOldDir;
    private File mCurrentLogFile;

    private ExecutorService mWriteExecutor;
    private ScheduledExecutorService mFlushScheduler;

    private final LinkedBlockingQueue<LogEntry> mWriteQueue = new LinkedBlockingQueue<>();
    private static final int FLUSH_INTERVAL_MS = 500;
    private static final int BATCH_SIZE = 100;

    private static DeviceInfoProvider sDeviceInfoProvider;

    private LogManager() {}

    /**
     * 设置设备信息提供者（在主模块中调用）
     */
    public static void setDeviceInfoProvider(DeviceInfoProvider provider) {
        sDeviceInfoProvider = provider;
    }

    public static void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        LogManager localInstance = sInstance;
        if (localInstance == null) {
            synchronized (LogManager.class) {
                localInstance = sInstance;
                if (localInstance == null) {
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
        this.mAppContext = context.getApplicationContext();
        this.mInitialized = true;

        // 初始化线程池
        mWriteExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogWriter");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        mFlushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogFlusher");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        initLogDirectories();

        // 异步加载历史日志
        mWriteExecutor.execute(this::loadHistoryLogs);
        startBatchWriter();

        AndroidLog.setLogListener((level, tag, message) -> {
            try {
                LogEntry entry = new LogEntry(level, "App", message, tag, true);
                addLog(entry);
            } catch (Throwable ignored) {
            }
        });

        // 添加初始化日志
        addLog(new LogEntry("I", "LogManager", "LogManager initialized", "LogManager", true));

        // 异步更新设备信息
        mWriteExecutor.execute(this::updateDeviceInfoSync);

        // 异步同步 Xposed 日志
        mWriteExecutor.execute(() -> {
            try {
                XposedLogLoader.loadLogsSync();
                Log.i(TAG, "Xposed log sync completed on startup");
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync Xposed logs on startup", e);
            }
        });
    }

    private void startBatchWriter() {
        mFlushScheduler.scheduleWithFixedDelay(() -> {
            try {
                flushWriteQueue();
            } catch (Exception e) {
                Log.e(TAG, "Batch write failed", e);
            }
        }, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void flushWriteQueue() {
        if (mWriteQueue.isEmpty()) return;

        List<LogEntry> batch = new ArrayList<>(BATCH_SIZE);
        mWriteQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) return;

        try {
            synchronized (LogManager.class) {
                File targetFile = getCurrentLogFile();
                if (!targetFile.equals(mCurrentLogFile)) {
                    mCurrentLogFile = targetFile;
                }

                try (FileOutputStream fos = new FileOutputStream(mCurrentLogFile, true);
                     BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {
                    for (LogEntry entry : batch) {
                        String line = entry.toLogFileLine() + "\n";
                        bos.write(line.getBytes());
                    }}
            }
        } catch (IOException e) {
            Log.e(TAG, "Batch save logs failed", e);
        }
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

    private void initLogDirectories() {
        mLogBaseDir = new File(mAppContext.getFilesDir(), LOG_BASE_DIR);
        mAppLogBaseDir = new File(mAppContext.getFilesDir(), APP_LOG_BASE_DIR);
        mAppLogDir = new File(mAppContext.getFilesDir(), APP_LOG_DIR);
        mAppLogOldDir = new File(mAppContext.getFilesDir(), APP_LOG_OLD_DIR);

        if (!mLogBaseDir.exists() && !mLogBaseDir.mkdirs()) {
            Log.w(TAG, "Failed to create log base directory: " + mLogBaseDir.getAbsolutePath());
        }
        if (!mAppLogBaseDir.exists() && !mAppLogBaseDir.mkdirs()) {
            Log.w(TAG, "Failed to create app log base directory: " + mAppLogBaseDir.getAbsolutePath());
        }
        if (!mAppLogDir.exists() && !mAppLogDir.mkdirs()) {
            Log.w(TAG, "Failed to create app log directory: " + mAppLogDir.getAbsolutePath());
        }

        // 创建或获取当前日志文件
        mCurrentLogFile = getCurrentLogFile();
    }

    /**
     * 获取当前日志文件
     * 文件名格式: app_yyyyMMdd.log
     */
    private File getCurrentLogFile() {
        String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String fileName = LOG_FILE_PREFIX + dateStr + LOG_FILE_SUFFIX;
        return new File(mAppLogDir, fileName);
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
            String deviceInfo = sDeviceInfoProvider.getDeviceInfo(mAppContext);
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
            if (!mLogBaseDir.exists() && !mLogBaseDir.mkdirs()) {
                Log.w(TAG, "Failed to create log base directory for device info");
                return;
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

    /**
     * 轮转应用日志（跟随 Xposed 日志轮转）
     */
    public void rotateAppLogs() {
        try {
            if (mAppLogOldDir.exists()) {
                deleteDirectory(mAppLogOldDir);
            }

            if (mAppLogDir.exists() && hasFiles(mAppLogDir)) {
                if (!mAppLogDir.renameTo(mAppLogOldDir)) {
                    Log.w(TAG, "Failed to rename log directory to log.old");
                }
            }

            if (!mAppLogDir.exists() && !mAppLogDir.mkdirs()) {
                Log.w(TAG, "Failed to recreate app log directory");
            }mCurrentLogFile = getCurrentLogFile();

            synchronized (mLogEntries) {
                mLogEntries.clear();}

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

    public void addLog(LogEntry entry) {
        if (!mInitialized) return;
        synchronized (mLogEntries) {
            mLogEntries.add(entry);
        }
        mWriteQueue.offer(entry);
    }

    public void addLogs(List<LogEntry> entries) {
        if (!mInitialized) return;
        synchronized (mLogEntries) {
            mLogEntries.addAll(entries);
        }

        for (LogEntry entry : entries) {
            mWriteQueue.offer(entry);
        }
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
        if (mAppLogDir.exists()) {
            deleteDirectory(mAppLogDir);
        }
        if (mAppLogOldDir.exists()) {
            deleteDirectory(mAppLogOldDir);
        }
        if (!mAppLogDir.mkdirs()) {
            Log.w(TAG, "Failed to recreate app log directory after clear");
        }
        mCurrentLogFile = getCurrentLogFile();
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
            XposedLogLoader.clearLogsStatic(mAppContext);
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

    public void shutdown() {
        flushWriteQueue();

        if (mFlushScheduler != null) {
            mFlushScheduler.shutdown();
        }
        if (mWriteExecutor != null) {
            mWriteExecutor.shutdown();
        }
    }

    private void loadHistoryLogs() {
        loadLogsFromDirectory(mAppLogOldDir);
        loadLogsFromDirectory(mAppLogDir);
    }

    private void loadLogsFromDirectory(File directory) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles((dir, name) ->
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

        if (files == null) return;

        java.util.Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            loadLogsFromFile(file);
        }
    }

    private void loadLogsFromFile(File file) {
        try {
            if (!file.exists()) return;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                StringBuilder currentEntry = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (isNewLogEntry(line)) {
                        if (!currentEntry.isEmpty()) {
                            LogEntry entry = parseLogLine(currentEntry.toString());
                            if (entry != null) {
                                synchronized (mLogEntries) {
                                    mLogEntries.add(entry);
                                }
                            }
                        }
                        currentEntry = new StringBuilder(line);
                    } else {
                        if (!currentEntry.isEmpty()) {
                            currentEntry.append("\n").append(line);
                        }
                    }
                }

                if (!currentEntry.isEmpty()) {
                    LogEntry entry = parseLogLine(currentEntry.toString());
                    if (entry != null) {
                        synchronized (mLogEntries) {
                            mLogEntries.add(entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Load logs from file failed: " + file.getName(), e);
        }
    }

    private boolean isNewLogEntry(String line) {
        return line.startsWith("[") && LOG_LINE_PATTERN.matcher(line).find();
    }

    private LogEntry parseLogLine(String line) {
        try {
            // 提取第一行用于解析元数据
            String firstLine = line.contains("\n") ? line.substring(0, line.indexOf("\n")) : line;
            Matcher matcher = LOG_LINE_PATTERN.matcher(firstLine);

            if (matcher.find()) {
                String timeStr = matcher.group(1);
                int uid = Integer.parseInt(matcher.group(2));
                int pid = Integer.parseInt(matcher.group(3));
                int tid = Integer.parseInt(matcher.group(4));
                String level = matcher.group(5);
                String tag = matcher.group(6).trim();
                String message = matcher.group(7);

                if (line.contains("\n")) {
                    message = message + line.substring(line.indexOf("\n"));
                }

                long timestamp = LOG_TIME_FORMAT.parse(timeStr).getTime();

                return new LogEntry(timestamp, level, "App", message, tag, true, uid, pid, tid);
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse log line failed: " + line, e);
        }
        return null;
    }

    /**
     * 创建日志压缩包到缓存目录
     * @return 压缩文件，失败返回 null
     */
    public File createLogZipFile() {
        try {
            updateDeviceInfoSync();

            File cacheDir = new File(mAppContext.getCacheDir(), "log_export");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.w(TAG, "Failed to create cache directory for log export");
                return null;
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
            OutputStream os = mAppContext.getContentResolver().openOutputStream(uri);
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
                if (!zipFile.delete()) {
                    Log.w(TAG, "Failed to delete temporary zip file: " + zipFile.getAbsolutePath());
                }
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

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            addDirectoryToZip(zos, sourceDir, sourceDir.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create zip file", e);
            return false;
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
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            ZipEntry entry = new ZipEntry(entryPath);
            entry.setTime(file.lastModified());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
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
            if (!files[i].delete()) {
                Log.w(TAG, "Failed to delete old export file: " + files[i].getAbsolutePath());
            }
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

    public Context getContext() {
        return mAppContext;
    }
}
