package com.sevtinge.hyperceiler.common.log

internal object LoggerUtils {
    fun shouldLog(logLevel: Int, requiredLevel: Int): Boolean = logLevel >= requiredLevel

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
        0 -> "Disable"
        1 -> "Error"
        2 -> "Warn"
        3 -> "Info"
        4 -> "Debug"
        else -> "Unknown"
    }
}
