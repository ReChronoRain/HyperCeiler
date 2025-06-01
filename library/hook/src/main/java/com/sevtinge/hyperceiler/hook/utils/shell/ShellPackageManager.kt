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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils.shell

import android.content.ComponentName
import android.util.Log
import com.sevtinge.hyperceiler.hook.BuildConfig

@Suppress("unused")
object ShellPackageManager {
    private const val TAG = "ShellPackageManager"
    private const val PM = "pm"

    private val DEBUG = BuildConfig.DEBUG

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

    // TODO: --user [USER_ID] 指定特定用户
    @JvmStatic
    fun enableOrDisable(packageName: String, isEnable: Boolean): Boolean {
        val status = if (isEnable) {
            "enable"
        } else {
            "disable"
        }

        val commandResult = ShellUtils.execCommand("$PM $status $packageName", true)
        if (DEBUG) {
            Log.d(TAG, commandResult.toString())
        }
        return commandResult.result == 0
    }

    @JvmStatic
    fun enableOrDisable(componentName: ComponentName, isEnable: Boolean): Boolean {
        return enableOrDisable(componentName.flattenToString(), isEnable)
    }
}
