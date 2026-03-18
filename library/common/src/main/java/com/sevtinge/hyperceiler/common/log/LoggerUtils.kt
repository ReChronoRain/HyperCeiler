package com.sevtinge.hyperceiler.common.log

internal object LoggerUtils {
    fun shouldLog(logLevel: Int, requiredLevel: Int): Boolean = when (logLevel) {
        LogLevelManager.LEVEL_DISABLED -> false
        LogLevelManager.LEVEL_ERROR_ONLY -> requiredLevel == LogLevelManager.LEVEL_ERROR_ONLY
        LogLevelManager.LEVEL_VERBOSE -> requiredLevel == LogLevelManager.LEVEL_ERROR_ONLY ||
            requiredLevel == LogLevelManager.LEVEL_VERBOSE
        else -> false
    }

    fun formatBrackets(pkg: String?, tag: String, message: String): String {
        return if (pkg.isNullOrEmpty()) {
            "[$tag]: $message"
        } else {
            "[$pkg][$tag]: $message"
        }
    }

    fun formatMessage(baseTag: String, level: String, message: String): String {
        if (message.contains("]: ")) {
            return "[$baseTag][$level]$message"
        }
        return "[$baseTag][$level]: $message"
    }

    fun logLevelDesc(level: Int): String = when (level) {
        LogLevelManager.LEVEL_DISABLED -> "Disabled"
        LogLevelManager.LEVEL_ERROR_ONLY -> "General"
        LogLevelManager.LEVEL_VERBOSE -> "Detailed"
        else -> "Unknown"
    }
}
