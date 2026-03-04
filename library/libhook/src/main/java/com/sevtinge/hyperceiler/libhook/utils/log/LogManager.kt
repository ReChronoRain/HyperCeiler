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

import com.sevtinge.hyperceiler.libhook.utils.log.LogManager.IS_LOGGER_ALIVE
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
     * 2. 执行 [onConfigReady] 回调 — 调用方应在此设置 [AndroidLog.setLogListener] 等
     * 3. 异步执行日志服务健康检查，结果写入 [IS_LOGGER_ALIVE]
     *
     * @param appPrivateDir 应用私有目录（用于日志配置文件读写）
     * @param onConfigReady 配置就绪后的同步回调，用于设置 LogListener 等。
     *                      在健康检查之前执行，确保 listener 已就绪。可为 null。
     */
    @JvmStatic
    @JvmOverloads
    fun init(appPrivateDir: String, onConfigReady: Runnable? = null) {
        // 初始化配置管理器
        LogConfigManager.init(appPrivateDir)
        onConfigReady?.run()

        // 异步健康检查
        Thread({
            try {
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
