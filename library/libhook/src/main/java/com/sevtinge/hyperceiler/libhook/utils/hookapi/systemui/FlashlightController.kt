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
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

@Suppress("unused")
class FlashlightController(instance: Any) : BaseReflectObject(instance) {
    fun isEnabled(): Boolean = instance.callMethod("isEnabled") as Boolean

    fun isAvailable(): Boolean = instance.callMethod("isAvailable") as Boolean

    // 是否支持官方 UI 调节亮度
    fun supportFlashlightUIDisplay(): Boolean = runCatching {
        instance.callMethod("supportFlashlightUIDisplay") as Boolean
    }.getOrDefault(false)

    fun setFlashlight(enabled: Boolean) {
        instance.callMethod("setFlashlight", enabled)
    }

    fun toggleFlashlight() = setFlashlight(!isEnabled())

    companion object {
        private val miuiFlashlightControllerImpl by lazy {
            loadClass("com.android.systemui.controlcenter.policy.MiuiFlashlightControllerImpl")
        }

        private var isHooked = false
        private val listeners = mutableListOf<FlashlightListener>()

        fun addListener(listener: FlashlightListener) {
            listeners += listener
            hookDispatchListeners()
        }

        fun removeListener(listener: FlashlightListener) {
            listeners -= listener
        }

        private fun hookDispatchListeners() {
            if (isHooked) {
                return
            }
            isHooked = true

            miuiFlashlightControllerImpl.findMethod { name("dispatchListeners"); parameterTypes(Int::class.java, Boolean::class.java) }
                .createAfterHook { param ->
                    synchronized(listeners) {
                        val event = param.args[0] as Int
                        val isEnabled = param.args[1] as Boolean

                        listeners.forEach {
                            when (event) {
                                0 -> it.onFlashlightError()
                                1 -> it.onFlashlightChanged(isEnabled)
                                2 -> it.onFlashlightAvailabilityChanged(isEnabled)
                            }
                        }
                    }
                }
        }
    }

    interface FlashlightListener {
        fun onFlashlightError() {}
        fun onFlashlightChanged(isEnabled: Boolean)
        fun onFlashlightAvailabilityChanged(isEnabled: Boolean) {}
    }
}
