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
package com.sevtinge.hyperceiler.common.log

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi.isRelease

/**
 * 日志级别管理器
 */
object LogLevelManager {
    @JvmStatic
    fun getEffectiveLogLevel(level: Int): Int {
        return if (isRelease()) {
            when (level) {
                0 -> 0
                1, 2, 3, 4 -> 1
                else -> 1
            }
        } else {
            when (level) {
                0, 1 -> 1
                2, 3, 4 -> 2
                else -> 2
            }
        }
    }

    @JvmStatic
    fun getDefaultLogLevel(): Int {
        if (isRelease()) {
            return 1
        }
        return 2
    }

    @JvmStatic
    fun getCurrentLogLevel(): Int {
        val level = PrefsBridge.getStringAsInt("log_level", getDefaultLogLevel())
        return getEffectiveLogLevel(level)
    }

    @JvmStatic
    fun logLevelDesc(level: Int): String {
        return when (level) {
            0 -> "Disabled"
            1 -> "General"
            2 -> "Detailed"
            else -> "Unknown"
        }
    }
}
