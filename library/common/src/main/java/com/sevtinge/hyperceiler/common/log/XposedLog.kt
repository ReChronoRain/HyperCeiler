package com.sevtinge.hyperceiler.common.log

import android.util.Log
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

    private fun shouldLog(requiredLevel: Int): Boolean {
        return LoggerUtils.shouldLog(LogStatusManager.getLogLevel(), requiredLevel)
    }

    // --- Full logs: 2 ---
    @JvmStatic
    fun d(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.DEBUG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.DEBUG, "[$tag]: $msg")
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.DEBUG, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun d(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.DEBUG, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    // --- Full logs: 2 ---
    @JvmStatic
    fun i(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.INFO, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.INFO, "[$tag]: $msg")
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.INFO, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    // --- Full logs: 2 ---
    @JvmStatic
    fun w(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.WARN, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.WARN, "[$tag]: $msg")
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.WARN, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.WARN, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        logRaw(Log.WARN, LoggerUtils.formatBrackets(pkg, tag, msg), t)
    }

    // --- Error only: 1 ---
    @JvmStatic
    fun e(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, "[$tag]: $msg")
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, "[$tag]: ${t.message ?: t.toString()}", t)
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, "[$tag]: $msg", t)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, LoggerUtils.formatBrackets(pkg, tag, msg))
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        logRaw(Log.ERROR, LoggerUtils.formatBrackets(pkg, tag, msg), t)
    }

    @JvmStatic
    fun logLevelDesc(): String {
        return LoggerUtils.logLevelDesc(LogStatusManager.getLogLevel())
    }
}
