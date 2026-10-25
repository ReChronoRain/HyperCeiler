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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.CarrierNetworkName
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
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
 *
 * 模式 5（真实网络名）在 OS3 与 OS4 两条 call 链上取同一个物理卡槽的注册网络名称，
 * 取数或调用失败都保持原文，不阻断系统界面的回调。
 */
object CustomCarrierText : BaseHook() {
    private const val ON_MEASURE = "onMeasure"
    private const val CARRIER_TEXT_CHANGED = "onCarrierTextChanged"
    private const val CARRIER_CALLBACK = "com.android.systemui.controlcenter.shade.ControlCenterCarrierText\$mCarrierTextCallback\$1"
    private const val CARRIER_LAYOUT = "com.android.systemui.controlcenter.shade.MiuiCarrierTextLayout"
    private const val HEADER_CONTROLLER = "com.android.systemui.controlcenter.shade.ControlCenterHeaderController"

    /** 模式 5：显示该卡槽真实注册的网络名称。 */
    private const val MODE_REAL_NETWORK_NAME = 5

    /** 模式 5 下用户为卡槽 1、卡槽 2 手填的名称，留空表示使用真实注册网络名。 */
    private val CUSTOM_NAME_KEYS = listOf(
        "system_ui_control_center_real_network_name_slot1",
        "system_ui_control_center_real_network_name_slot2"
    )


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
            MODE_REAL_NETWORK_NAME -> hideCarrierSeparator()
        }
    }

    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun hookOS4() {
        val mode = PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0)
        if (mode !in 1..5) return
        val active = AtomicBoolean(false)
        val handles = ArrayList<XposedInterface.HookHandle>()
        try {
            val callback = exactMethod(
                requiredClass(CARRIER_CALLBACK),
                CARRIER_TEXT_CHANGED, Int::class.java, String::class.java, Int::class.java
            )
            val additional: Method?
            val views: List<java.lang.reflect.Field>
            if (mode == 1) {
                val layout = requiredClass(CARRIER_LAYOUT)
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
                        // 模式 5 在无服务时原文就是空串，必须仍然参与替换（取不到名称时保留原文）。
                        if (mode != 3 && mode != MODE_REAL_NETWORK_NAME && text.isNullOrEmpty()) return
                        param.args[1] = when (mode) {
                            1 -> text!!.replace("  |  ", "").replace(" | ", "")
                            2 -> ""
                            MODE_REAL_NETWORK_NAME -> realNetworkName(slot) ?: text
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

            // 取不到真实名称时回落到原文，与其它模式的"取不到就保持原样"一致。
            MODE_REAL_NETWORK_NAME -> realNetworkName(slotId) ?: carrierText
            else -> carrierText
        }
    }

    /**
     * 模式 5 取数：先看用户为该卡槽手填的名称，留空再取该卡槽真实注册的网络名称。
     * 取不到 Context 或取数失败都返回 null，由调用方保留 SystemUI 原文。
     */
    private fun realNetworkName(callbackSlot: Int): String? {
        customNetworkName(callbackSlot)?.let { return it }
        val context = EzXposed.appContextOrNull ?: return null
        return CarrierNetworkName.resolve(context, callbackSlot)
    }

    /** 读取用户在设置里为某个卡槽手填的名称；未填写或只有空白都视为没填。 */
    private fun customNetworkName(slot: Int): String? =
        CUSTOM_NAME_KEYS.getOrNull(slot)
            ?.let { key -> PrefsBridge.getString(key, "") }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

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
