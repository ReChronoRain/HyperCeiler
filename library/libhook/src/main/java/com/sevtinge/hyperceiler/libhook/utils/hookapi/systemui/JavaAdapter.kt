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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import java.lang.reflect.Modifier
import java.util.concurrent.CancellationException
import java.util.function.Consumer

class JavaAdapter(instance: Any) : BaseReflectObject(instance) {
    private companion object {
        const val COLLECT_FLOW_METHOD = "alwaysCollectFlow"
    }

    fun <T> alwaysCollectFlow(flow: Any, consumer: Consumer<T>): KotlinJob {
        val adapterClass = instance.javaClass
        // HyperOS 4 SystemUI 17.03.260226.r:
        // alwaysCollectFlow(Flow, Consumer) is void. Calling it reflectively
        // launches collection, but returns null; wrapping that null as KotlinJob
        // throws after the subscription has already been created.
        // The native implementation delegates to its static three-argument
        // overload with applicationScope, which DOES return StandaloneCoroutine.
        val voidOverload = isMoreAndroidVersion(37) && adapterClass.methods.any { method ->
            method.name == COLLECT_FLOW_METHOD &&
                method.parameterCount == 2 &&
                method.parameterTypes[1] == Consumer::class.java &&
                method.returnType == Void.TYPE
        }
        val nativeJob = if (voidOverload) {
            val applicationScopeField = adapterClass.getDeclaredField("applicationScope")
                .apply { isAccessible = true }
            val applicationScope = requireNotNull(applicationScopeField.get(instance)) {
                "SystemUI JavaAdapter.applicationScope is null"
            }
            val withScope = adapterClass.declaredMethods.single { method ->
                method.name == COLLECT_FLOW_METHOD &&
                    method.parameterCount == 3 &&
                    Modifier.isStatic(method.modifiers)
            }.apply { isAccessible = true }
            requireNotNull(withScope.invoke(null, applicationScope, flow, consumer)) {
                "SystemUI JavaAdapter scoped collector returned no Job"
            }
        } else {
            // Preserve the existing implementation for Android versions whose
            // public two-argument overload already returns a Job.
            requireNotNull(instance.callMethod(COLLECT_FLOW_METHOD, flow, consumer)) {
                "SystemUI JavaAdapter collector returned no Job"
            }
        }
        val job = KotlinJob(nativeJob)
        // Flow collector captures the module generation; cancel it on hot reload.
        BaseHook.registerHotReloadCleanup { job.cancel() }
        return job
    }
}

class KotlinJob(instance: Any) : BaseReflectObject(instance) {
    fun cancel(e: CancellationException? = null) {
        instance.callMethod("cancel", e)
    }
}
