package com.sevtinge.hyperceiler.common.log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 日志管理器 (Java 版本)
 */
public class LogStatusManager {

    public static volatile boolean IS_LOGGER_ALIVE = false;

    private static final CountDownLatch healthCheckLatch = new CountDownLatch(1);
    private static final List<Runnable> listeners = new ArrayList<>();
    private static final Object lock = new Object();
    private static boolean checkDone = false;

    public static int logLevel = readLogLevelFromFile();

    public static void init(String appPrivateDir, Runnable xposedLogSyncer, Runnable onConfigReady) {
        // 1. 初始化配置管理器
        LogConfigManager.init(appPrivateDir);

        // 2. 设置本地日志基准目录
        LoggerHealthChecker.localLogBaseDir = new File(appPrivateDir, "files/log");

        // 3. 配置就绪回调
        if (onConfigReady != null) {
            onConfigReady.run();
        }

        // 4. 开启异步线程进行健康检查
        new Thread(() -> {
            try {
                // 执行 Xposed 日志同步逻辑
                if (xposedLogSyncer != null) {
                    xposedLogSyncer.run();
                }

                // 检查日志服务存活状态
                IS_LOGGER_ALIVE = LoggerHealthChecker.isLoggerAlive();
                notifyListeners();
            } finally {
                healthCheckLatch.countDown();
            }
        }, "LogHealthCheck").start();
    }

    /**
     * 获取当前日志级别
     */
    public static int getLogLevel() {
        return readLogLevelFromFile();
    }

    /**
     * 注册回调，健康检查完成后立即在调用线程触发。
     */
    public static void onHealthCheckDone(Runnable listener) {
        synchronized (lock) {
            if (checkDone) {
                listener.run();
                return;
            }
            listeners.add(listener);
        }
    }

    private static void notifyListeners() {
        synchronized (lock) {
            checkDone = true;
            for (Runnable listener : listeners) {
                listener.run();
            }
            listeners.clear();
        }
    }

    public static boolean awaitHealthCheck(long timeoutMs) {
        try {
            return healthCheckLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取简短的日志服务状态
     */
    public static String formatLoggerStatus() {
        return LoggerHealthChecker.formatStatus();
    }

    /**
     * 获取详细的日志服务状态
     */
    public static String formatLoggerStatusDetail() {
        return LoggerHealthChecker.formatDetailedStatus();
    }

    public static void setLogLevel(int level) {
        int effectiveLogLevel = LogLevelManager.getEffectiveLogLevel(level);
        LogConfigManager.writeLogLevel(effectiveLogLevel);
    }

    public static void setLogLevel(int level, String basePath) {
        int effectiveLogLevel = LogLevelManager.getEffectiveLogLevel(level);
        LogConfigManager.writeLogLevel(basePath, effectiveLogLevel);
    }

    public static int readLogLevelFromFile() {
        return LogConfigManager.readLogLevel();
    }

    public static int readLogLevelFromFile(String basePath) {
        return LogConfigManager.readLogLevel(basePath);
    }

    public static String logLevelDesc() {
        return LogLevelManager.logLevelDesc(getLogLevel());
    }

    public static String fixLSPosedLogService() {
        return LoggerHealthChecker.fixLSPosedLogService();
    }
}
