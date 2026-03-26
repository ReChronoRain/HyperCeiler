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
package com.sevtinge.hyperceiler.hook.utils.hidden

import android.content.Context
import android.util.ArrayMap
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.api.BaseReflectObject
import com.sevtinge.hyperceiler.hook.utils.api.invokeMethod
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethodAs
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logI
import de.robv.android.xposed.XposedHelpers

object PackageWatchdog {
    private lateinit var classLoader: ClassLoader

    private val PACKAGE_WATCHDOG by lazy {
        logI(classLoader.toString())
        XposedHelpers.findClass("com.android.server.PackageWatchdog", classLoader)
    }

    fun getInstance(context: Context): Stub = Stub(
        PACKAGE_WATCHDOG.callStaticMethodAs("getInstance", context)
    )

    fun clearRecord(context: Context, packageName: String) {
        getInstance(context).allObservers.forEach { (_, v) ->
            val pkg = v.callMethod("getMonitoredPackage", packageName) ?: return@forEach
            MonitoredPackageStub(pkg).mitigationCalls!!.invokeMethod("clear")
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

