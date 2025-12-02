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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.MethodHandleUtils
import com.sevtinge.hyperceiler.hook.utils.PropUtils
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callMethodAs
import com.sevtinge.hyperceiler.hook.utils.getIntField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.setIntField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

/**
 * OS3 控制中心运行商名称自定义
 */
object CustomCarrierText : BaseHook() {
    override fun init() {
        when (mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0)) {
            1 -> hideCarrierSeparator()
            2 -> hideCarrierText()
            3 -> showDeviceName()
        }
    }

    // 隐藏分割线
    private fun hideCarrierSeparator() {
        loadClass("com.android.systemui.controlcenter.shade.MiuiCarrierTextLayout").methodFinder()
            .filterByName("onMeasure")
            .filterByParamTypes(Int::class.java, Int::class.java)
            .first().createBeforeHook { param ->
                val viewGroup = param.thisObject as ViewGroup
                val widthMeasureSpec = param.args[0] as Int
                val heightMeasureSpec = param.args[1] as Int

                if (!viewGroup.isVisible) {
                    // super.onMeasure
                    // 在 API 100 可间接调用 XposedModule#invokeSpecial
                    MethodHandleUtils.invokeSuperMethod(
                        viewGroup, "onMeasure",
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
                MethodHandleUtils.invokeSuperMethod(
                    viewGroup, "onMeasure",
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
        loadClass("com.android.systemui.controlcenter.shade.ControlCenterHeaderController")
            .methodFinder()
            .filterByName("updateCarrierAndPrivacyVisible")
            .first().createAfterHook { param ->
                param.thisObject.getObjectFieldAs<View>("carrierLayout").visibility = View.INVISIBLE
            }
    }

    // 显示设备名称
    private fun showDeviceName() {
        loadClass($$"com.android.systemui.controlcenter.shade.ControlCenterCarrierText$mCarrierTextCallback$1")
            .methodFinder()
            .filterByName("onCarrierTextChanged")
            .filterByParamTypes(Int::class.java, Int::class.java, String::class.java)
            .first().createBeforeHook { param ->
                val carrierText = param.thisObject.getObjectFieldAs<Any>("this$0")
                val slotId = carrierText.getIntField("innerCarrierSlotId")

                param.args[2] = if (slotId == 0) { // 只显示第一张卡的名称
                    PropUtils.getProp("persist.private.device_name");
                } else { // 其它置空隐藏
                    null
                }
            }
    }
}
