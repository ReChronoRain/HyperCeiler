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

import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isBeta
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isCanary
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap

/**
 * 日志级别管理器
 */
object LogLevelManager {
    /**
     * 根据构建类型获取有效的日志等级
     * Release: 0 (Disable) 或 1 (Error)
     * Beta: 1 (Error) 或 4 (Debug)
     * Canary: 3 (Info) 或 4 (Debug)
     * Debug: 0-4 全部
     */
    @JvmStatic
    fun getEffectiveLogLevel(level: Int): Int {
        if (isRelease()) {
            return if (level == 0) 0 else 1
        } else if (isBeta()) {
            return if (level == 1) 1 else 4
        } else if (isCanary()) {
            return if (level == 4) 4 else 3
        }
        return level
    }

    @JvmStatic
    fun getCurrentLogLevel(): Int {
        val level = mPrefsMap.getStringAsInt("log_level", 3)
        return getEffectiveLogLevel(level)
    }

    @JvmStatic
    fun logLevelDesc(level: Int): String {
        return when (level) {
            0 -> "Disable"
            1 -> "Error"
            2 -> "Warn"
            3 -> "Info"
            4 -> "Debug"
            else -> "Unknown"
        }
    }
}
