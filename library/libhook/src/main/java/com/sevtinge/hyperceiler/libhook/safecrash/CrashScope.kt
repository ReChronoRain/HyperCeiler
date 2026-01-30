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
package com.sevtinge.hyperceiler.libhook.safecrash

import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils

object CrashScope {
    // 包名 -> 简写别名
    private val scopeMap: Map<String, String> by lazy {
        mapOf(
            "com.android.systemui" to "systemui",
            "com.android.settings" to "settings",
            "com.miui.home" to "home",
            "com.miui.securitycenter" to "center",
            "com.hchen.demo" to "demo"
        )
    }

    // 简写别名 -> 包名
    private val swappedMap: Map<String, String> by lazy {
        scopeMap.entries.associate { (k, v) -> v to k }
    }

    fun getAlias(packageName: String): String? = scopeMap[packageName]

    fun getPackageName(alias: String): String? = swappedMap[alias]

    fun isScopeApp(packageName: String?): Boolean = scopeMap.containsKey(packageName)

    /**
     * 获取当前处于 Crash 状态的应用包名列表 (从属性读取)
     */
    @JvmStatic
    fun getCrashingPackages(): List<String> {
        val prop = PropUtils.getProp("persist.service.hyperceiler.crash.report", "")
        if (prop.isEmpty()) return emptyList()

        return prop.split(",").mapNotNull { alias ->
            swappedMap[alias]
        }
    }
}
