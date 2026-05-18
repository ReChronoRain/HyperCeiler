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
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setIntField

/**
 * OS3 控制中心运行商名称自定义
 */
object CustomCarrierText : BaseHook() {

    val getOperator by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0)
    }

    override fun init() {
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

    private fun hookLegacyCarrierText() {
        runCatching {
            loadClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView").findMethod { name("onCarrierTextChanged") }.createBeforeHook { param ->
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
            loadClass($$"com.android.systemui.controlcenter.shade.ControlCenterCarrierText$mCarrierTextCallback$1").findMethod { name("onCarrierTextChanged") }.createBeforeHook { param ->
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
        loadClass("com.android.systemui.controlcenter.shade.MiuiCarrierTextLayout").findMethod { name("onMeasure"); parameterTypes(Int::class.java, Int::class.java) }.createBeforeHook { param ->
                val viewGroup = param.thisObject as ViewGroup
                val widthMeasureSpec = param.args[0] as Int
                val heightMeasureSpec = param.args[1] as Int

                if (!viewGroup.isVisible) {
                    // super.onMeasure
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeSuperMethod(
                        "onMeasure", viewGroup,
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
                    "onMeasure", viewGroup,
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
        loadClass("com.android.systemui.controlcenter.shade.ControlCenterHeaderController").findMethod { name("updateCarrierAndPrivacyVisible") }.createAfterHook { param ->
                param.thisObject.getObjectFieldAs<View>("carrierLayout").visibility = View.INVISIBLE
            }
    }
}
