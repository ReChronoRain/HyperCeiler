/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setIntField

/**
 * 控制中心运营商名称自定义；设置变更后重启系统界面。
 */
object CustomCarrierText : BaseHook() {
    private const val ON_MEASURE = "onMeasure"
    private const val CARRIER_TEXT_CHANGED = "onCarrierTextChanged"
    private const val CARRIER_CALLBACK = "com.android.systemui.controlcenter.shade.ControlCenterCarrierText\$mCarrierTextCallback\$1"
    private const val CARRIER_LAYOUT = "com.android.systemui.controlcenter.shade.MiuiCarrierTextLayout"
    private const val HEADER_CONTROLLER = "com.android.systemui.controlcenter.shade.ControlCenterHeaderController"


    val getOperator by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0)
    }

    override fun init() {
        if (isMoreHyperOSVersion(4f)) {
            hookOS4()
            return
        }
        loadClass("com.android.systemui.statusbar.policy.HDController").findMethod { name("isVisible") }.createBeforeHook { param ->
                param.result = false
            }

        hookLegacyCarrierText()
        hookModernCarrierText()

        when (getOperator) {
            1 -> hideCarrierSeparator()
            2 -> hideCarrierText()
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun hookOS4() {
        val mode = PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0)
        if (mode !in 1..3) return
        val active = AtomicBoolean(false)
        val handles = ArrayList<XposedInterface.HookHandle>()
        try {
            val callback = exactMethod(
                requiredClass(CARRIER_CALLBACK),
                CARRIER_TEXT_CHANGED, Int::class.java, String::class.java, Int::class.java
            )
            val layout = requiredClass(CARRIER_LAYOUT)
            val additional: Method?
            val views: List<java.lang.reflect.Field>
            if (mode == 1) {
                additional = exactMethod(layout, ON_MEASURE, Int::class.java, Int::class.java)
                views = listOf(viewField(layout, "carrierSeparatorView"), viewField(layout, "carrierSeparatorText"))
            } else if (mode == 2) {
                val header = requiredClass(HEADER_CONTROLLER)
                val privacy = "com.android.systemui.plugins.miui.statusbar.MiuiPrivacyController"
                additional = exactMethod(header, "updateCarrierAndPrivacyVisible",
                    requiredClass(privacy + "\$MiuiPromptInfo"), requiredClass(privacy + "\$AndroidPromptInfo"))
                views = listOf(viewField(header, "carrierLayout"))
            } else {
                additional = null
                views = emptyList()
            }
            handles.add(checkNotNull(hookMethod(callback, object : IMethodHook {
                // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
                @Suppress("TooGenericExceptionCaught")
                override fun before(param: HookParam) {
                    if (!active.get() || param.isSkipped) return
                    try {
                        // ROM contract: available carrier count, text, physical SIM slot. Never replace either int.
                        val carrierCount = param.args[0] as? Int ?: return
                        val text = param.args[1] as? String
                        val slot = param.args[2] as? Int ?: return
                        if (carrierCount !in 0..2 || slot !in 0..1) return
                        if (mode != 3 && text.isNullOrEmpty()) return
                        param.args[1] = when (mode) {
                            1 -> text!!.replace("  |  ", "").replace(" | ", "")
                            2 -> ""
                            // Keep one fixed display slot even when only SIM2 is active. The
                            // producer deduplicates each text using a shared carrier count.
                            // A count-dependent display slot can miss the second callback.
                            else -> if (carrierCount == 0) text else {
                                val name = PropUtils.getProp("persist.private.device_name")
                                if (name.isNullOrBlank()) text else if (slot == 0) name else ""
                            }
                        }
                    } catch (error: RuntimeException) {
                        debugCallbackError("OS4 carrier text", error)
                    }
                }
            })))
            if (additional != null) {
                handles.add(checkNotNull(hookMethod(additional, object : IMethodHook {
                    override fun after(param: HookParam) {
                        if (!active.get() || param.hasThrowable) return
                        try {
                            // Preserve the ROM's full measurement/privacy/accessibility work.
                            // INVISIBLE keeps its measured spacing; a GONE view stays GONE.
                            for (field in views) {
                                val view = field.get(param.thisObject) as? View ?: continue
                                if (view.visibility == View.VISIBLE) view.visibility = View.INVISIBLE
                            }
                        } catch (error: ReflectiveOperationException) {
                            debugCallbackError("OS4 carrier visibility", error)
                        } catch (error: RuntimeException) {
                            debugCallbackError("OS4 carrier visibility", error)
                        }
                    }
                })))
            }
            active.set(true)
        } catch (error: Exception) {
            active.set(false)
            for (handle in handles) {
                try { handle.unhook() } catch (rollback: RuntimeException) { error.addSuppressed(rollback) }
            }
            throw IllegalStateException("OS4 carrier callback shape or registration unavailable", error)
        }
    }

    private fun requiredClass(name: String): Class<*> =
        findClassIfExists(name) ?: throw IllegalStateException("Missing OS4 carrier class: $name")

    private fun exactMethod(owner: Class<*>, name: String, vararg args: Class<*>): Method =
        owner.getDeclaredMethod(name, *args).also {
            check(it.returnType == Void.TYPE && !Modifier.isStatic(it.modifiers))
            it.isAccessible = true
        }

    private fun viewField(owner: Class<*>, name: String): java.lang.reflect.Field =
        owner.getDeclaredField(name).also {
            check(View::class.java.isAssignableFrom(it.type) && !Modifier.isStatic(it.modifiers))
            it.isAccessible = true
        }

    private fun hookLegacyCarrierText() {
        runCatching {
            loadClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView").findMethod { name(CARRIER_TEXT_CHANGED) }.createBeforeHook { param ->
                    param.args[2] = transformCarrierText(
                        slotId = param.args[1] as Int,
                        carrierText = param.args[2] as? String
                    )
                }
        }
    }

    // 显示设备名称
    private fun hookModernCarrierText() {
        runCatching {
            loadClass(CARRIER_CALLBACK).findMethod { name(CARRIER_TEXT_CHANGED) }.createBeforeHook { param ->
                    param.args[2] = transformCarrierText(
                        slotId = param.args[1] as Int,
                        carrierText = param.args[2] as? String
                    )
                }
        }
    }

    private fun transformCarrierText(slotId: Int, carrierText: String?): String? {
        return when (getOperator) {
            1 -> carrierText
                ?.replace("  |  ", "")
                ?.replace(" | ", "")

            2 -> ""
            3 -> if (slotId == 0) {
                PropUtils.getProp("persist.private.device_name")
            } else {
                null
            }

            else -> carrierText
        }
    }

    // 隐藏分割线
    private fun hideCarrierSeparator() {
        loadClass(CARRIER_LAYOUT).findMethod { name(ON_MEASURE); parameterTypes(Int::class.java, Int::class.java) }.createBeforeHook { param ->
                val viewGroup = param.thisObject as ViewGroup
                val widthMeasureSpec = param.args[0] as Int
                val heightMeasureSpec = param.args[1] as Int

                if (!viewGroup.isVisible) {
                    // super.onMeasure
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeSuperMethod(
                        ON_MEASURE, viewGroup,
                        widthMeasureSpec,
                        heightMeasureSpec
                    )
                    param.result = null
                    return@createBeforeHook
                }

                var availableWidth = View.MeasureSpec.getSize(widthMeasureSpec)
                if (viewGroup.callMethodAs("getQsHeaderLayout")) {
                    availableWidth /= 2
                }
                viewGroup.setIntField("availableWidth", availableWidth)

                viewGroup.getObjectFieldAs<View>("carrierSeparatorView").isVisible = false
                val leftCarrierTextView = viewGroup.getObjectFieldAs<View>("leftCarrierTextView")
                val rightCarrierTextView = viewGroup.getObjectFieldAs<View>("rightCarrierTextView")
                if (leftCarrierTextView.callMethodAs("shouldShow")) {
                    viewGroup.callMethod("setCarrierMaxWidth", leftCarrierTextView, availableWidth)
                } else {
                    viewGroup.callMethod("setCarrierMaxWidth", rightCarrierTextView, availableWidth)
                }

                // super.onMeasure
                com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeSuperMethod(
                    ON_MEASURE, viewGroup,
                    View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                    heightMeasureSpec
                )
                viewGroup.callMethod(
                    "setMeasuredDimension",
                    availableWidth,
                    viewGroup.measuredHeight
                )

                param.result = null
            }
    }

    // 隐藏全部名称
    private fun hideCarrierText() {
        loadClass(HEADER_CONTROLLER).findMethod { name("updateCarrierAndPrivacyVisible") }.createAfterHook { param ->
                param.thisObject.getObjectFieldAs<View>("carrierLayout").visibility = View.INVISIBLE
            }
    }
}
