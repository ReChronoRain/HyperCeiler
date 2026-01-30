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

    private fun logRaw(msg: String, t: Throwable? = null) {
        val xposed = sXposed
        if (msg.isNotEmpty()) {
            if (xposed != null) {
                xposed.log(msg)
            } else {
                Log.i(TAG, msg)
            }
        }
        if (t != null) {
            if (xposed != null) {
                xposed.log(Log.getStackTraceString(t))
            } else {
                Log.e(TAG, "get xposed failed", t)
            }
        }
    }

    // ============ Debug: 4 ============
    @JvmStatic
    fun d(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(LoggerUtils.formatMessage(TAG, "D", msg))
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "D", tag, msg))
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "D", tag, msg), t)
    }

    @JvmStatic
    fun d(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "D", pkg, tag, msg))
    }

    // ============ Info: 3 ============
    @JvmStatic
    fun i(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(LoggerUtils.formatMessage(TAG, "I", msg))
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "I", tag, msg))
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "I", pkg, tag, msg))
    }

    // ============ Warn: 2 ============
    @JvmStatic
    fun w(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(LoggerUtils.formatMessage(TAG, "W", msg))
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "W", tag, msg))
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "W", tag, msg), t)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "W", pkg, tag, msg))
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "W", pkg, tag, msg), t)
    }

    // ============ Error: 1 ============
    @JvmStatic
    fun e(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessage(TAG, "E", msg))
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "E", tag, msg))
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "E", tag, t.message ?: t.toString()), t)
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessageWithTag(TAG, "E", tag, msg), t)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "E", pkg, tag, msg))
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        logRaw(LoggerUtils.formatMessageWithPkg(TAG, "E", pkg, tag, msg), t)
    }

    @JvmStatic
    fun logLevelDesc(): String {
        return LoggerUtils.logLevelDesc(logLevel)
    }
}
