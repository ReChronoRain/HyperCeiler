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

import java.io.File
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
    private val listeners = mutableListOf<Runnable>()
    private val lock = Any()
    private var checkDone = false
    val logLevel: Int
        get() = readLogLevelFromFile()

    @JvmStatic
    fun init(appPrivateDir: String, xposedLogSyncer: Runnable?, onConfigReady: Runnable?) {
        LogConfigManager.init(appPrivateDir)
        LoggerHealthChecker.localLogBaseDir = File(appPrivateDir, "files/log")
        onConfigReady?.run()

        Thread({
            try {
                xposedLogSyncer?.run()
                IS_LOGGER_ALIVE = LoggerHealthChecker.isLoggerAlive()
                notifyListeners()
            } finally {
                healthCheckLatch.countDown()
            }
        }, "LogHealthCheck").start()
    }

    /**
     * 注册回调，健康检查完成后立即在调用线程触发。
     * 如果注册时检查已完成，立即执行。
     */
    @JvmStatic
    fun onHealthCheckDone(listener: Runnable) {
        synchronized(lock) {
            if (checkDone) {
                listener.run()
                return
            }
            listeners.add(listener)
        }
    }

    private fun notifyListeners() {
        synchronized(lock) {
            checkDone = true
            listeners.forEach { it.run() }
            listeners.clear()
        }
    }

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
