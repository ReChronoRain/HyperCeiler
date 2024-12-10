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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.utils.*

import de.robv.android.xposed.XposedHelpers.*

@Suppress("MemberVisibilityCanBePrivate")
object Dependency {
    private const val DEPENDENCY = "com.android.systemui.Dependency"

    private val dependencyClz by lazy {
        findClass(DEPENDENCY, EzXHelper.classLoader)
    }

    /* ========================== only for HyperOS2 ========================== */
    @JvmStatic
    @get:JvmName(name = "getDependency")
    val sDependency: Any?
        get() = dependencyClz.getStaticObjectField("sDependency")

    @JvmStatic
    @get:JvmName(name = "getMiuiLegacyDependency")
    val mMiuiLegacyDependency: Any?
        get() = sDependency?.getObjectField("mMiuiLegacyDependency")

    @JvmStatic
    @get:JvmName(name = "getDependencies")
    val mDependencies: Map<*, *>?
        get() = sDependency?.getObjectField("mDependencies") as Map<*, *>?

    @JvmStatic
    fun getDependencyInner(depClz: Class<*>): Any? {
        return sDependency?.callMethod("getDependencyInner", depClz)
    }

    @JvmStatic
    fun getDependencyInner(depClzName: String): Any? {
        return getDependencyInner(findClass(depClzName, EzXHelper.classLoader))
    }

    /* ========================== only for HyperOS1 ========================== */
    @JvmStatic
    fun get(depClz: Class<*>): Any? {
        return dependencyClz.callStaticMethod("get", depClz)
    }
}