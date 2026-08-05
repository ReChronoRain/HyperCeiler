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
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import java.util.concurrent.CancellationException
import java.util.function.Consumer

class JavaAdapter(instance: Any) : BaseReflectObject(instance) {
    fun <T> alwaysCollectFlow(flow: Any, consumer: Consumer<T>): KotlinJob? {
        val job = KotlinJob(startCollect(flow, consumer) ?: return null)
        // Flow collector 持有 Consumer，而 Consumer 往往捕获当前模块 generation。
        // 不在热重载前取消会让旧 classloader 持续接收状态更新。
        BaseHook.registerHotReloadCleanup { job.cancel() }
        return job
    }

    /**
     * Starts collecting and returns a cancellable Job where possible.
     *
     * The instance method `alwaysCollectFlow(Flow, Consumer)` used to return a Job. On
     * HyperOS 3.3 (Android 17) its return type became void -- the body just passes
     * applicationScope to the static overload of the same name and discards the result --
     * so the original `as Any` threw a NullPointerException and collection was never set
     * up at all.
     *
     * Check the instance method's return type first: if it is not void, use it as before;
     * if it is void, call that static overload directly with applicationScope so a
     * StandaloneCoroutine is still available for hot-reload cancellation. Exactly one of
     * the two paths may run, otherwise the same Flow would be collected twice.
     */
    private fun <T> startCollect(flow: Any, consumer: Consumer<T>): Any? {
        // methods (not declaredMethods) so an inherited overload is still found
        val instanceMethod = instance.javaClass.methods.firstOrNull {
            it.name == "alwaysCollectFlow" && it.parameterCount == 2
        }

        if (instanceMethod != null && instanceMethod.returnType != Void.TYPE) {
            return runCatching {
                instance.callMethod("alwaysCollectFlow", flow, consumer)
            }.getOrNull()
        }

        runCatching {
            val scope = instance.getObjectField("applicationScope")
            instance.javaClass.callStaticMethod("alwaysCollectFlow", scope, flow, consumer)
        }.getOrNull()?.let { return it }

        // If the static overload did not work either, fall back to the void instance
        // method: collection is still established, there is just no Job to cancel.
        runCatching { instance.callMethod("alwaysCollectFlow", flow, consumer) }
        return null
    }
}

class KotlinJob(instance: Any) : BaseReflectObject(instance) {
    fun cancel(e: CancellationException? = null) {
        instance.callMethod("cancel", e)
    }
}
