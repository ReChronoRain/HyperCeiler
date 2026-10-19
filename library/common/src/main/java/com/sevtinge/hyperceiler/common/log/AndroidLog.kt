package com.sevtinge.hyperceiler.common.log

import android.util.Log

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
        runCatching {
            sLogListener?.onLog(level, tag, message)
        }
    }

    // App records already delivered to the database listener must not be imported twice.
    // Calls from hooked processes (which have no listener) use the same identifiable wire tag.
    private fun write(priority: Int, tag: String, message: String, error: Throwable? = null) {
        val wireTag = if (sLogListener != null) "HyperCeiler.App.$tag" else ModuleLog.PREFIX + tag
        Log.println(priority, wireTag, message + (error?.let { "\n" + Log.getStackTraceString(it) } ?: ""))
    }

    private fun shouldLog(requiredLevel: Int): Boolean {
        return LoggerUtils.shouldLog(LogStatusManager.getLogLevel(), requiredLevel)
    }

    // ============ Full logs: 2 ============
    @JvmStatic
    fun d(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.DEBUG, TAG, msg)
        notifyListener("D", TAG, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.DEBUG, tag, "[$tag]: $msg")
        notifyListener("D", tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, t: Throwable?) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.DEBUG, tag, "[$tag]: $msg", t)
        notifyListener("D", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun d(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.DEBUG, tag, LoggerUtils.formatBrackets(pkg, tag, msg))
        notifyListener("D", tag, msg)
    }

    // ============ Full logs: 2 ============
    @JvmStatic
    fun i(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.INFO, TAG, msg)
        notifyListener("I", TAG, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.INFO, tag, "[$tag]: $msg")
        notifyListener("I", tag, msg)
    }

    @JvmStatic
    fun i(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.INFO, tag, LoggerUtils.formatBrackets(pkg, tag, msg))
        notifyListener("I", tag, msg)
    }

    // ============ Full logs: 2 ============
    @JvmStatic
    fun w(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.WARN, TAG, msg)
        notifyListener("W", TAG, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.WARN, tag, "[$tag]: $msg")
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable?) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.WARN, tag, "[$tag]: $msg", t)
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.WARN, tag, LoggerUtils.formatBrackets(pkg, tag, msg))
        notifyListener("W", tag, msg)
    }

    @JvmStatic
    fun w(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (!shouldLog(LogLevelManager.LEVEL_VERBOSE)) return
        write(Log.WARN, tag, LoggerUtils.formatBrackets(pkg, tag, msg), t)
        notifyListener("W", tag, msg + if (t != null) "\n$t" else "")
    }

    // ============ Error only: 1 ============
    @JvmStatic
    fun e(msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, TAG, msg)
        notifyListener("E", TAG, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, tag, "[$tag]: $msg")
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, t: Throwable) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, tag, "[$tag]: ${t.message ?: t.toString()}", t)
        notifyListener("E", tag, t.message ?: t.toString())
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable?) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, tag, "[$tag]: $msg", t)
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, tag, LoggerUtils.formatBrackets(pkg, tag, msg))
        notifyListener("E", tag, msg)
    }

    @JvmStatic
    fun e(tag: String, pkg: String?, msg: String, t: Throwable?) {
        if (!shouldLog(LogLevelManager.LEVEL_ERROR_ONLY)) return
        write(Log.ERROR, tag, LoggerUtils.formatBrackets(pkg, tag, msg), t)
        notifyListener("E", tag, msg + if (t != null) "\n$t" else "")
    }
}
