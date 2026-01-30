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

/**
 * 日志管理器
 *
 */
object LogManager {

    @JvmField
    var IS_LOGGER_ALIVE: Boolean = false

    val logLevel: Int
        get() = readLogLevelFromFile()

    @JvmStatic
    fun init(appPrivateDir: String) {
        // 初始化配置管理器
        LogConfigManager.init(appPrivateDir)

        // 检查日志服务
        IS_LOGGER_ALIVE = LoggerHealthChecker.isLoggerAlive()
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
