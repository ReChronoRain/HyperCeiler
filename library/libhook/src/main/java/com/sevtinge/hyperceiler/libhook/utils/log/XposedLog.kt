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

import android.util.Log
import com.sevtinge.hyperceiler.libhook.utils.log.LogManager.logLevel
import io.github.libxposed.api.XposedInterface

/**
 * Xposed 日志工具类
 */
object XposedLog {
    private const val TAG = "HyperCeiler"

    @Volatile
    private var sXposed: XposedInterface? = null

    @JvmStatic
    fun init(xposed: XposedInterface) {
        sXposed = xposed
    }

    @Suppress("DEPRECATION")
    private fun logRaw(priority: Int, msg: String, t: Throwable? = null) {
        val xposed = sXposed
        if (xposed != null) {
            try {
                xposed.log(priority, TAG, msg, t)
            } catch (_: NoSuchMethodError) {
                val oldMsg =
                    LoggerUtils.formatMessage(TAG, priorityToLevel(priority), msg)

                if (t != null) {
                    xposed.log(oldMsg, t)
                } else {
                    xposed.log(oldMsg)
                }
            }
        } else {
            Log.println(priority, TAG, msg)
            t?.let { Log.println(priority, TAG, Log.getStackTraceString(it)) }
        }
    }

    private fun priorityToLevel(priority: Int): String = when (priority) {
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "V"
    }

    // --- Debug: 4 ---
    @JvmStatic
    fun d(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(Log.DEBUG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(Log.DEBUG, "[$tag]: $msg")
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(Log.DEBUG, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun d(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(Log.DEBUG, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    // --- Info: 3 ---
    @JvmStatic
    fun i(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(Log.INFO, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(Log.INFO, "[$tag]: $msg")
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(Log.INFO, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    // --- Warn: 2 ---
    @JvmStatic
    fun w(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(Log.WARN, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(Log.WARN, "[$tag]: $msg")
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(Log.WARN, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(Log.WARN, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(Log.WARN, LoggerUtils.formatBrackets(pkg, tag, msg), t)
    }

    // --- Error: 1 ---
    @JvmStatic
    fun e(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, "[$tag]: $msg")
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, "[$tag]: ${t.message ?: t.toString()}", t)
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(Log.ERROR, LoggerUtils.formatBrackets(pkg, tag, msg), t)
    }

    @JvmStatic
    fun logLevelDesc(): String {
        return LoggerUtils.logLevelDesc(logLevel)
    }
}
