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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui

import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getStaticObjectFieldAs

@Suppress("unused")
object Dependency {
    private val CLASS by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("com.android.systemui.Dependency")
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
    fun getDependencyInner(className: String): Any? = getDependencyInner(com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass(className))

    /* ========================== only for HyperOS1 ========================== */
    val activityStarter by lazy {
        ActivityStarter(
            INSTANCE.getObjectFieldAs<Any>("mActivityStarter").callMethod("get") as Any
        )
    }

    @JvmStatic
    fun get(clazz: Class<*>): Any? = CLASS.callStaticMethod("get", clazz)
}
