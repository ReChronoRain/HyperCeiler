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

/**
 * Android 日志工具类
 *
 * @author HyperCeiler
 */
object AndroidLog {
    private const val TAG = "HyperCeiler"

    interface LogListener {
        fun onLog(level: String, tag: String, message: String)
    }

    @Volatile
    private var sLogListener: LogListener? = null

    @JvmStatic
    fun setLogListener(listener: LogListener?) {
        sLogListener = listener
    }

    private fun notifyListener(level: String, tag: String, message: String) {
        try {
            sLogListener?.onLog(level, tag, message)
        } catch (_: Throwable) {
        }
    }

    // ============ Debug: 4 ============
    @JvmStatic
    fun d(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        Log.d(TAG, LoggerUtils.formatMessage(TAG, "D", msg))
        notifyListener("D", TAG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        Log.d(tag, LoggerUtils.formatMessageWithTag(TAG, "D", tag, msg))
        notifyListener("D", tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable?) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        Log.d(tag, LoggerUtils.formatMessageWithTag(TAG, "D", tag, msg), t)
        notifyListener("D", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun d(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 4)) return
        Log.d(tag, LoggerUtils.formatMessageWithPkg(TAG, "D", pkg, tag, msg))
        notifyListener("D", tag, msg)
    }

    // ============ Info: 3 ============
    @JvmStatic
    fun i(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        Log.i(TAG, LoggerUtils.formatMessage(TAG, "I", msg))
        notifyListener("I", TAG, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        Log.i(tag, LoggerUtils.formatMessageWithTag(TAG, "I", tag, msg))
        notifyListener("I", tag, msg)
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 3)) return
        Log.i(tag, LoggerUtils.formatMessageWithPkg(TAG, "I", pkg, tag, msg))
        notifyListener("I", tag, msg)
    }

    // ============ Warn: 2 ============
    @JvmStatic
    fun w(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        Log.w(TAG, LoggerUtils.formatMessage(TAG, "W", msg))
        notifyListener("W", TAG, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        Log.w(tag, LoggerUtils.formatMessageWithTag(TAG, "W", tag, msg))
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable?) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        Log.w(tag, LoggerUtils.formatMessageWithTag(TAG, "W", tag, msg), t)
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        Log.w(tag, LoggerUtils.formatMessageWithPkg(TAG, "W", pkg, tag, msg))
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (!LoggerUtils.shouldLog(logLevel, 2)) return
        Log.w(tag, LoggerUtils.formatMessageWithPkg(TAG, "W", pkg, tag, msg), t)
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    // ============ Error: 1 ============
    @JvmStatic
    fun e(msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(TAG, LoggerUtils.formatMessage(TAG, "E", msg))
        notifyListener("E", TAG, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(tag, LoggerUtils.formatMessageWithTag(TAG, "E", tag, msg))
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(tag, LoggerUtils.formatMessageWithTag(TAG, "E", tag, t.message ?: t.toString()), t)
        notifyListener("E", tag, t.message ?: t.toString())
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable?) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(tag, LoggerUtils.formatMessageWithTag(TAG, "E", tag, msg), t)
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(tag, LoggerUtils.formatMessageWithPkg(TAG, "E", pkg, tag, msg))
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (!LoggerUtils.shouldLog(logLevel, 1)) return
        Log.e(tag, LoggerUtils.formatMessageWithPkg(TAG, "E", pkg, tag, msg), t)
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun logLevelDesc(): String {
        return LoggerUtils.logLevelDesc(logLevel)
    }
}

