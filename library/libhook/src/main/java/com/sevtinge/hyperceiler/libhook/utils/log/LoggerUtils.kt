/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.log


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
