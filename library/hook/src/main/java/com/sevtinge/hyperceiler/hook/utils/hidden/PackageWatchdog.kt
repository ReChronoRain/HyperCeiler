package com.sevtinge.hyperceiler.hook.utils.hidden

import android.content.Context
import android.util.ArrayMap
import android.util.LongArrayQueue
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.BaseReflectObject
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethodAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil

object PackageWatchdog {
    private val PACKAGE_WATCHDOG by lazy {
        ClassUtil.loadClass("com.android.server.PackageWatchdog")
    }

    fun getInstance(context: Context): Stub = Stub(
        PACKAGE_WATCHDOG.callStaticMethodAs("getInstance", context)
    )

    fun clearRecord(context: Context, packageName: String) {
        getInstance(context).allObservers.forEach { (_, v) ->
            val pkg = v.callMethod("getMonitoredPackage", packageName) ?: return@forEach
            MonitoredPackageStub(pkg).mitigationCalls.clear()
        }
    }

    class Stub(instance: Any): BaseReflectObject(instance) {
        val allObservers by lazy {
            getObjectFieldAs<ArrayMap<String, *>>("mAllObservers")
        }
    }

    class MonitoredPackageStub(instance: Any): BaseReflectObject(instance) {
        val mitigationCalls by lazy {
            getObjectFieldAs<LongArrayQueue>("mMitigationCalls")
        }
    }
}

