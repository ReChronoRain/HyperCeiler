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
    const val PREF_KEY = "log_level_v2"
    const val LEVEL_DISABLED = 0
    const val LEVEL_ERROR_ONLY = 1
    const val LEVEL_VERBOSE = 2

    @JvmStatic
    fun getEffectiveLogLevel(level: Int): Int {
        return if (isRelease()) {
            when (level) {
                LEVEL_DISABLED -> LEVEL_DISABLED
                LEVEL_ERROR_ONLY, LEVEL_VERBOSE -> LEVEL_ERROR_ONLY
                else -> getDefaultLogLevel()
            }
        } else {
            when (level) {
                LEVEL_DISABLED, LEVEL_ERROR_ONLY -> LEVEL_ERROR_ONLY
                LEVEL_VERBOSE -> LEVEL_VERBOSE
                else -> getDefaultLogLevel()
            }
        }
    }

    @JvmStatic
    fun getDefaultLogLevel(): Int {
        return LEVEL_ERROR_ONLY
    }

    @JvmStatic
    fun getCurrentLogLevel(): Int {
        val level = PrefsBridge.getStringAsInt(PREF_KEY, getDefaultLogLevel())
        return getEffectiveLogLevel(level)
    }

    @JvmStatic
    fun logLevelDesc(level: Int): String {
        return when (level) {
            LEVEL_DISABLED -> "Disabled"
            LEVEL_ERROR_ONLY -> "General"
            LEVEL_VERBOSE -> "Detailed"
            else -> "Unknown"
        }
    }
}
