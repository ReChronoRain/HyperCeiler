package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callMethodAs

@Suppress("unused")
class FlashlightController(instance: Any) : BaseReflectObject(instance) {
    fun isEnabled(): Boolean = instance.callMethodAs("isEnabled")

    fun isAvailable(): Boolean = instance.callMethodAs("isAvailable")

    // 是否支持官方 UI 调节亮度
    fun supportFlashlightUIDisplay(): Boolean = runCatching {
        instance.callMethodAs<Boolean>("supportFlashlightUIDisplay")
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

            miuiFlashlightControllerImpl.methodFinder()
                .filterByName("dispatchListeners")
                .filterByParamTypes(Int::class.java, Boolean::class.java)
                .single().createAfterHook { param ->
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
