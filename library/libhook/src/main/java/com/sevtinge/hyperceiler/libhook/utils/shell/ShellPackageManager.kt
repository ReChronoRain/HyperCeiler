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
package com.sevtinge.hyperceiler.libhook.utils.shell

import android.content.ComponentName
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog

/**
 * 包启用/禁用工具
 */
@Suppress("unused")
object ShellPackageManager {
    private const val TAG = "ShellPackageManager"
    private const val PM = "pm"

    @JvmStatic
    fun enable(packageName: String): Boolean {
        return enableOrDisable(packageName, true)
    }

    @JvmStatic
    fun enable(componentName: ComponentName): Boolean {
        return enableOrDisable(componentName.flattenToString(), true)
    }

    @JvmStatic
    fun disable(packageName: String): Boolean {
        return enableOrDisable(packageName, false)
    }

    @JvmStatic
    fun disable(componentName: ComponentName): Boolean {
        return enableOrDisable(componentName.flattenToString(), false)
    }

    @JvmStatic
    fun enableOrDisable(packageName: String, isEnable: Boolean): Boolean {
        val status = if (isEnable) "enable" else "disable"
        val commandResult = ShellUtils.execCommand("$PM $status $packageName", true)
        if (ProjectApi.isDebug()) {
            AndroidLog.d(TAG, commandResult.toString())
        }
        return commandResult.result == 0
    }

    @JvmStatic
    fun enableOrDisable(componentName: ComponentName, isEnable: Boolean): Boolean {
        return enableOrDisable(componentName.flattenToString(), isEnable)
    }
}

