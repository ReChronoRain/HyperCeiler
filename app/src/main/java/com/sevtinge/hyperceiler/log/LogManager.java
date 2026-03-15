package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.room.Room;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogDatabase;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import com.sevtinge.hyperceiler.logviewer.DeviceInfoProvider;
import com.sevtinge.hyperceiler.logviewer.XposedLogLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogManager {

    private static final String TAG = "LogManager";

    private static volatile LogManager sInstance;

    private Context mAppContext;
    private static DeviceInfoProvider sDeviceInfoProvider;

    public static LogManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("LogManager must be initialized in Application!");
        }
        return sInstance;
    }

    public static void init(Context context) {
        if (sInstance == null) {
            synchronized (LogManager.class) {
                if (sInstance == null) sInstance = new LogManager(context);
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
        addLog(new LogEntry("App", "I", "LogManager", "LogManager 初始化成功"));
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
            (level == null || level.equals("ALL")) ? "ALL" : level,
            (tag == null || tag.equals("全部标签")) ? "全部标签" : tag,
            keyword == null ? "" : keyword
        );
    }

    /**
     * 清空所有日志
     */
    public void clearAllLogs() {
        LogRepository.getInstance().clearAllLogs();
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
            entry.getMessage());
    }

    /**
     * 导出日志压缩包到指定 Uri (数据库版实现)
     */
    public boolean exportLogsZipToUri(Uri uri) {
        File tempDir = new File(mAppContext.getCacheDir(), "log_export_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) return false;

        File zipFile = new File(mAppContext.getCacheDir(), "temp_logs.zip");

        try {
            // 1. 生成设备信息文件
            File deviceInfoFile = new File(tempDir, "devices.txt");
            String deviceInfo = (sDeviceInfoProvider != null) ?
                sDeviceInfoProvider.getDeviceInfo(mAppContext) : "No device info provider set.";
            Files.write(deviceInfoFile.toPath(), deviceInfo.getBytes());

            // 2. 从数据库提取日志并写入文件
            File logFile = new File(tempDir, "all_logs.log");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                // 分批次查询防止 OOM，这里简单处理拿全部，如果日志极多建议分段查询
                List<LogEntry> allLogs = LogRepository.getInstance().getDao().queryLogs("ALL", "ALL", "全部标签", "");
                for (LogEntry entry : allLogs) {
                    writer.write(formatLogEntryForFile(entry));
                    writer.newLine();
                }
            }

            // 3. 压缩目录
            if (createZipFromDirectory(tempDir, zipFile)) {
                // 4. 将压缩后的文件流写入 Uri
                try (InputStream is = new FileInputStream(zipFile);
                     OutputStream os = mAppContext.getContentResolver().openOutputStream(uri)) {
                    if (os == null) return false;
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Export logs failed", e);
        } finally {
            // 清理临时文件
            deleteDirectory(tempDir);
            if (zipFile.exists()) zipFile.delete();
        }
        return false;
    }

    /**
     * 格式化日志用于文件输出，模拟之前的日志行格式
     */
    private String formatLogEntryForFile(LogEntry entry) {
        // 格式: [2026-03-15T16:30:00.000 1000:1234:5678 I/TagName] Message
        return String.format("[%s %s/%s] %s",
            entry.getFormattedTime(),
            entry.getLevel(),
            entry.getTag(),
            entry.getMessage());
    }

    /**
     * 压缩目录工具 (保持你原有的逻辑即可，只需简单封装)
     */
    private boolean createZipFromDirectory(File sourceDir, File zipFile) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File[] files = sourceDir.listFiles();
            if (files == null) return false;
            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        dir.delete();
    }

}
