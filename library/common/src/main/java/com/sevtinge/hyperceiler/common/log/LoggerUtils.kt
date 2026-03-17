package com.sevtinge.hyperceiler.common.log

internal object LoggerUtils {
    fun shouldLog(logLevel: Int, requiredLevel: Int): Boolean = when (logLevel) {
        0 -> false
        1 -> requiredLevel == 1
        2 -> true
        else -> requiredLevel == 1
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
        0 -> "Disabled"
        1 -> "General"
        2 -> "Detailed"
        else -> "Unknown"
    }
}
