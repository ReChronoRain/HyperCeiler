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
package com.sevtinge.hyperceiler.libhook.utils.log

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 日志管理器
 */
object LogManager {

    @JvmField
    @Volatile
    var IS_LOGGER_ALIVE: Boolean = false

    private val healthCheckLatch = CountDownLatch(1)

    val logLevel: Int
        get() = readLogLevelFromFile()

    /**
     * 初始化日志系统（唯一入口）。
     *
     * 执行流程：
     * 1. 初始化日志配置
     * 2. 执行 [onConfigReady] 回调 — 调用方应在此设置 LogListener、LogManager 等
     * 3. 异步：先执行 [xposedLogSyncer]（同步 Xposed 日志到本地），再执行健康检查
     *
     * @param appPrivateDir   应用私有目录（用于日志配置文件读写）
     * @param xposedLogSyncer 同步 Xposed 日志到本地的回调（在后台线程中执行，可含 root 操作）。
     *                        健康检查会在其完成后读取本地日志文件，避免自身依赖 root。
     * @param onConfigReady   配置就绪后的同步回调，在健康检查之前执行。可为 null。
     */
    @JvmStatic
    fun init(appPrivateDir: String, xposedLogSyncer: Runnable?, onConfigReady: Runnable?) {
        // 初始化配置管理器
        LogConfigManager.init(appPrivateDir)
        // 设置本地日志目录供健康检查使用
        LoggerHealthChecker.localLogBaseDir = java.io.File(appPrivateDir, "files/log")
        onConfigReady?.run()

        // 异步：同步 Xposed 日志 → 健康检查
        Thread({
            try {
                xposedLogSyncer?.run()
                IS_LOGGER_ALIVE = LoggerHealthChecker.isLoggerAlive()
            } finally {
                healthCheckLatch.countDown()
            }
        }, "LogHealthCheck").start()
    }

    /**
     * 等待健康检查完成。
     * 注意：不要在主线程调用，会阻塞 UI。
     *
     * @param timeoutMs 最大等待时间（毫秒），0 表示不等待立即返回当前状态
     * @return 健康检查是否在超时前完成
     */
    @JvmStatic
    fun awaitHealthCheck(timeoutMs: Long): Boolean {
        return try {
            healthCheckLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    /**
     * 获取简短的日志服务状态（用于弹窗等 UI 展示）
     * 示例："HEALTHY(100%)" / "DEAD(0%)"
     */
    @JvmStatic
    fun formatLoggerStatus(): String {
        return LoggerHealthChecker.formatStatus()
    }

    /**
     * 获取详细的日志服务状态（用于设备信息/调试输出）
     * 示例："HEALTHY(100%) logcat=OK xp_mod=OK xp_verb=OK"
     */
    @JvmStatic
    fun formatLoggerStatusDetail(): String {
        return LoggerHealthChecker.formatDetailedStatus()
    }

    @JvmStatic
    fun setLogLevel(level: Int) {
        val effectiveLogLevel = LogLevelManager.getEffectiveLogLevel(level)
        LogConfigManager.writeLogLevel(effectiveLogLevel)
    }

    @JvmStatic
    fun setLogLevel(level: Int, basePath: String?) {
        val effectiveLogLevel = LogLevelManager.getEffectiveLogLevel(level)
        LogConfigManager.writeLogLevel(basePath, effectiveLogLevel)
    }

    @JvmStatic
    fun readLogLevelFromFile(): Int {
        return LogConfigManager.readLogLevel()
    }

    @JvmStatic
    fun readLogLevelFromFile(basePath: String?): Int {
        return LogConfigManager.readLogLevel(basePath)
    }

    @JvmStatic
    fun logLevelDesc(): String {
        return LogLevelManager.logLevelDesc(logLevel)
    }

    @JvmStatic
    fun fixLSPosedLogService(): String {
        return LoggerHealthChecker.fixLSPosedLogService()
    }
}
