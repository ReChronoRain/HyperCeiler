package com.fan.common.logviewer;

// LogManager.java

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogManager {

    private static LogManager sInstance;
    private final List<LogEntry> mLogEntries;
    private final List<LogEntry> mSystemLogEntries;
    private Context mApplicationContext;
    private boolean mIsInitialized = false;

    private static final String sLogFileName = "custom_logs.txt";
    private static final String sSystemLogFileName = "system_logs.txt";

    // 私有构造函数
    private LogManager() {
        // 延迟初始化，等待setApplicationContext调用
        mLogEntries = new ArrayList<>();
        mSystemLogEntries = new ArrayList<>();
    }

    /**
     * 推荐方式：通过Application初始化
     */
    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (LogManager.class) {
                if (sInstance == null) {
                    sInstance = new LogManager();
                    sInstance.setApplicationContext(context.getApplicationContext());
                }
            }
        }
    }

    /**
     * 兼容方式：传统getInstance（内部会检查是否已初始化）
     */
    public static LogManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LogManager.class) {
                if (sInstance == null) {
                    sInstance = new LogManager();
                    sInstance.setApplicationContext(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    /**
     * 获取实例（必须在initialize之后调用）
     */
    public static LogManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("LogManager must be initialized first. " +
                    "Call LogManager.initialize(context) in your Application class.");
        }
        return sInstance;
    }

    private void setApplicationContext(Context applicationContext) {
        this.mApplicationContext = applicationContext;
        this.mIsInitialized = true;
        loadHistoryLogs();

        // 记录初始化日志
        addLog(new LogEntry("I", "LogManager", "LogManager initialized with Application Context",
                "System", true));
    }

    // 确保在使用前已初始化
    private void checkInitialization() {
        if (!mIsInitialized || mApplicationContext == null) {
            throw new IllegalStateException("LogManager not properly initialized. " +
                    "Make sure to call initialize() in your Application class.");
        }
    }

    // 添加自定义日志
    // 修改所有需要Context的方法，添加检查
    public void addLog(LogEntry logEntry) {
        checkInitialization();
        mLogEntries.add(logEntry);
        saveLogToFile(logEntry, sLogFileName);
    }

    // 获取系统日志
    public void captureSystemLogs() {
        checkInitialization();
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d -v time");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                mSystemLogEntries.clear();
                while ((line = bufferedReader.readLine()) != null) {
                    LogEntry logEntry = parseLogcatLine(line);
                    if (logEntry != null) {
                        mSystemLogEntries.add(logEntry);
                    }
                }
                saveSystemLogsToFile();
            } catch (IOException e) {
                Log.e("LogManager", "Error reading logcat", e);
            }
        }).start();
    }

    private LogEntry parseLogcatLine(String line) {
        // 简化解析，实际需要更复杂的解析逻辑
        if (line.contains(" E ")) {
            return new LogEntry("E", "System", line, "System", true);
        } else if (line.contains(" W ")) {
            return new LogEntry("W", "System", line, "System", true);
        } else if (line.contains(" I ")) {
            return new LogEntry("I", "System", line, "System", true);
        } else if (line.contains(" D ")) {
            return new LogEntry("D", "System", line, "System", true);
        } else {
            return new LogEntry("V", "System", line, "System", true);
        }
    }

    // 清空日志
    public void clearCustomLogs() {
        mLogEntries.clear();
        deleteLogFile(sLogFileName);
    }

    public void clearSystemLogs() {
        mSystemLogEntries.clear();
        deleteLogFile(sSystemLogFileName);
    }

    // 导出日志
    public boolean exportLogs(String fileName, boolean includeSystemLogs) {
        checkInitialization();
        try {
            File exportFile = new File(mApplicationContext.getExternalFilesDir(null), fileName);
            FileOutputStream outputStream = new FileOutputStream(exportFile);

            // 写入自定义日志
            for (LogEntry entry : mLogEntries) {
                String logLine = String.format("%s %s/%s: %s\n",
                        entry.getFormattedTime(),
                        entry.getModule(),
                        entry.getLevel(),
                        entry.getMessage());
                outputStream.write(logLine.getBytes());
            }

            // 写入系统日志
            if (includeSystemLogs) {
                for (LogEntry entry : mSystemLogEntries) {
                    String logLine = String.format("%s %s\n",
                            entry.getFormattedTime(),
                            entry.getMessage());
                    outputStream.write(logLine.getBytes());
                }
            }

            outputStream.close();
            return true;
        } catch (IOException e) {
            Log.e("LogManager", "Export failed", e);
            return false;
        }
    }

    private void saveLogToFile(LogEntry logEntry, String fileName) {
        new Thread(() -> {
            try {
                FileOutputStream outputStream = mApplicationContext.openFileOutput(
                        fileName, Context.MODE_APPEND);
                String logLine = String.format("%d|%s|%s|%s|%s|%b\n",
                        logEntry.getTimestamp(),
                        logEntry.getLevel(),
                        logEntry.getModule(),
                        logEntry.getMessage(),
                        logEntry.getTag(),
                        logEntry.isNewLine());
                outputStream.write(logLine.getBytes());
                outputStream.close();
            } catch (IOException e) {
                Log.e("LogManager", "Save log failed", e);
            }
        }).start();
    }

    private void saveSystemLogsToFile() {
        new Thread(() -> {
            try {
                FileOutputStream outputStream = mApplicationContext.openFileOutput(
                        sSystemLogFileName, Context.MODE_PRIVATE);
                for (LogEntry entry : mSystemLogEntries) {
                    String logLine = String.format("%d|%s|%s|%s|%s|%b\n",
                            entry.getTimestamp(),
                            entry.getLevel(),
                            entry.getModule(),
                            entry.getMessage(),
                            entry.getTag(),
                            entry.isNewLine());
                    outputStream.write(logLine.getBytes());
                }
                outputStream.close();
            } catch (IOException e) {
                Log.e("LogManager", "Save system logs failed", e);
            }
        }).start();
    }

    private void loadHistoryLogs() {
        loadLogsFromFile(sLogFileName, mLogEntries);
        loadLogsFromFile(sSystemLogFileName, mSystemLogEntries);
    }

    private void loadLogsFromFile(String fileName, List<LogEntry> targetList) {
        try {
            FileInputStream inputStream = mApplicationContext.openFileInput(fileName);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 6) {
                    LogEntry logEntry = new LogEntry(
                            parts[1], parts[2], parts[3], parts[4], Boolean.parseBoolean(parts[5])
                    );
                    targetList.add(logEntry);
                }
            }
            reader.close();
        } catch (IOException e) {
            // 文件不存在是正常的
        }
    }

    private void deleteLogFile(String fileName) {
        File file = new File(mApplicationContext.getFilesDir(), fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    // Getters
    public List<LogEntry> getLogEntries() { return mLogEntries; }
    public List<LogEntry> getSystemLogEntries() { return mSystemLogEntries; }
}
