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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.devicesdk.getAndroidVersion
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object BatteryStyle : BaseHook() {
    private val fontSize by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_size", 15) * 0.5f
    }
    private val fontSizeMark by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_mark_size", 15) * 0.5f
    }
    private val verticalOffset by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset", 8)
    }
    private val verticalOffsetMark by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset_mark", 27)
    }
    private val isChangeLocation by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location")
    }
    private val isHideText by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")
    }
    private val isEnableCustom by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom")
    }
    private val isEnableBold by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_bold")
    }
    private val isEnableBatteryMark by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")
    }

    private val mBatteryMeterViewClass = when {
        getAndroidVersion() >= 31 -> loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView")
        else -> loadClass("com.android.systemui.MiuiBatteryMeterView")
    }

    override fun init() {
        if (isAndroidVersion(30)) {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateResources")
                .single()
        } else {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateAll")
                .single()
        }.createHook {
            after { param ->
                hookStatusBattery(param)
            }
        }
    }

    private fun changeLocation(batteryView: LinearLayout, mBatteryPercentView: TextView, mBatteryPercentMarkView: TextView) {
        batteryView.removeView(mBatteryPercentView)
        batteryView.removeView(mBatteryPercentMarkView)
        batteryView.addView(mBatteryPercentMarkView, 0)
        batteryView.addView(mBatteryPercentView, 0)
    }

    private fun setBatterySize(view: TextView, size: Float) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }

    private fun setMargin(view1: TextView, view2: TextView) {
        // 左侧间距
        var leftMargin =
            mPrefsMap.getInt("system_ui_status_bar_battery_style_left_margin", 0)
        leftMargin = dp2px(leftMargin * 0.5f)

        // 右侧间距
        var rightMargin =
            mPrefsMap.getInt("system_ui_status_bar_battery_style_right_margin", 0)
        rightMargin = dp2px(rightMargin * 0.5f)

        // 上下偏移量
        var topMargin = 0
        if (verticalOffset != 12) {
            topMargin = dp2px((verticalOffset - 12) * 0.5f)
        }
        view1.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

        var digitRightMargin = 0
        var markRightMargin = 0
        if (isEnableBatteryMark) {
            digitRightMargin = rightMargin
        } else {
            markRightMargin = rightMargin
        }
        if (leftMargin > 0 || topMargin != 8 || digitRightMargin > 0) {
            view1.setPaddingRelative(
                leftMargin, topMargin, digitRightMargin, 0
            )
        }

        if (verticalOffsetMark < 27) {
            val marginTop =
                dp2px((verticalOffsetMark - 8) * 0.5f)
            topMargin = marginTop
        }
        if (verticalOffsetMark < 27 || markRightMargin > 0) {
            view2.setPaddingRelative(0, topMargin, markRightMargin, 0)
        }
    }

    private fun hookStatusBattery(param: XC_MethodHook.MethodHookParam) {
        val batteryView = param.thisObject as LinearLayout
        val mBatteryPercentView=
            XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView") as TextView
        val mBatteryPercentMarkView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView") as TextView
        val mBatteryTextDigitView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryTextDigitView") as TextView

        // 交换电池图标与电量位置（电量外显下才能正常交换）
        if (isChangeLocation) {
            changeLocation(batteryView, mBatteryPercentView, mBatteryPercentMarkView)
        }

        // 以下功能需要启用修改
        if (!isHideText && isEnableCustom) {
            if (fontSize > 7.5) {
                setBatterySize(mBatteryTextDigitView, fontSize)
                setBatterySize(mBatteryPercentView, fontSize)
            }
            if (fontSizeMark > 7.5) {
                setBatterySize(mBatteryPercentMarkView, fontSizeMark)
            }

            if (isEnableBold) {
                mBatteryTextDigitView.typeface = Typeface.DEFAULT_BOLD
                mBatteryPercentView.typeface = Typeface.DEFAULT_BOLD
            }

            // 设置边距
           setMargin(mBatteryPercentView, mBatteryPercentMarkView)
        }
    }
}
