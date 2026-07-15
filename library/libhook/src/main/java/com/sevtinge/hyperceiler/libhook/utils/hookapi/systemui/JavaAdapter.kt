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
import java.util.concurrent.CancellationException
import java.util.function.Consumer

class JavaAdapter(instance: Any) : BaseReflectObject(instance) {
    fun <T> alwaysCollectFlow(flow: Any, consumer: Consumer<T>): KotlinJob {
        val job = KotlinJob(instance.callMethod("alwaysCollectFlow", flow, consumer) as Any)
        // Flow collector 持有 Consumer，而 Consumer 往往捕获当前模块 generation。
        // 不在热重载前取消会让旧 classloader 持续接收状态更新。
        BaseHook.registerHotReloadCleanup { job.cancel() }
        return job
    }
}

class KotlinJob(instance: Any) : BaseReflectObject(instance) {
    fun cancel(e: CancellationException? = null) {
        instance.callMethod("cancel", e)
    }
}
