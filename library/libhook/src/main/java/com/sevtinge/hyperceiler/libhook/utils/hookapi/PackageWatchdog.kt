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
package com.sevtinge.hyperceiler.libhook.utils.hookapi

import android.content.Context
import android.util.ArrayMap
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.BaseReflectObject
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass as findClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs

object PackageWatchdog {
    private lateinit var classLoader: ClassLoader

    private val PACKAGE_WATCHDOG by lazy {
        XposedLog.i(classLoader.toString())
        findClass("com.android.server.PackageWatchdog", classLoader)
    }

    fun getInstance(context: Context): Stub = Stub(
        PACKAGE_WATCHDOG.callStaticMethod("getInstance", context) as Any
    )

    fun clearRecord(context: Context, packageName: String) {
        getInstance(context).allObservers.forEach { (_, v) ->
            val pkg = v.callMethod("getMonitoredPackage", packageName) ?: return@forEach
            MonitoredPackageStub(pkg).mitigationCalls!!.callMethod("clear")
        }
    }

    fun setClassLoader(cl: ClassLoader) {
        classLoader = cl
    }

    class Stub(instance: Any) : BaseReflectObject(instance) {
        val allObservers by lazy {
            instance.getObjectFieldAs<ArrayMap<String, *>>("mAllObservers")
        }
    }

    class MonitoredPackageStub(instance: Any) : BaseReflectObject(instance) {
        val mitigationCalls by lazy {
            instance.getObjectField("mMitigationCalls")
        }
    }
}

