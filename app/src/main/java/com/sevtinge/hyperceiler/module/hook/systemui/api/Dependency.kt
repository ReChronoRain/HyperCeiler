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
package com.sevtinge.hyperceiler.module.hook.systemui.api

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.utils.*

@Suppress("unused")
object Dependency {
    private const val DEPENDENCY = "com.android.systemui.Dependency"

    private val CLASS by lazy {
        loadClass(DEPENDENCY)
    }

    val INSTANCE by lazy {
        CLASS.getStaticObjectFieldAs<Any>("sDependency")
    }

    val dependencies by lazy {
        INSTANCE.getObjectFieldAs<Map<*, *>>("mDependencies")
    }

    /* ========================== only for HyperOS2 ========================== */
    val miuiLegacyDependency by lazy {
        INSTANCE.getObjectField("mMiuiLegacyDependency")
    }

    @JvmStatic
    fun getDependencyInner(clazz: Class<*>): Any? = INSTANCE.callMethod("getDependencyInner", clazz)

    @JvmStatic
    fun getDependencyInner(className: String): Any? = getDependencyInner(loadClass(className))

    /* ========================== only for HyperOS1 ========================== */
    val activityStarter by lazy {
        ActivityStarter(
            INSTANCE.getObjectFieldAs<Any>("mActivityStarter").callMethodAs("get")
        )
    }

    @JvmStatic
    fun get(clazz: Class<*>): Any? = CLASS.callStaticMethod("get", clazz)
}
