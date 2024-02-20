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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.devicesdk.getAndroidVersion

object TimeStyle : BaseHook() {
    private val clockBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_bold")
    }
    private val getMode by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0)
    }
    private val isAlign by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_mode", 0)
    }
    private val isGeekAlign by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_mode_geek", 0)
    }
    private val verticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset", 12)
    }
    private val isClockDouble by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_double")
    }
    private val lineSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_double_spacing_margin", 16)
    }
    private val lineSpacingGeek by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_geek_spacing_margin", 16)
    }

    private var leftMargin =
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin", 0)
    private var rightMargin =
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin", 0)

    private val mClockClass = when {
        getAndroidVersion() >= 31 -> loadClass("com.android.systemui.statusbar.views.MiuiClock")
        else -> loadClass("com.android.systemui.statusbar.policy.MiuiClock")
    }

    @SuppressLint("RtlHardcoded", "DiscouragedApi")
    override fun init() {
        mClockClass.constructorFinder()
            .filterByParamCount(3).single().createHook {
                after {
                    val textV = it.thisObject as TextView

                    if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                        // 时钟加粗
                        if (clockBold) {
                            textV.typeface = Typeface.DEFAULT_BOLD
                        }

                        // 时钟边距调整
                        margin(textV)

                        // 以下 Hook 需要启用自定义时钟指示器才能生效
                        if (getMode != 0) {
                            val alignment = when (getMode) {
                                1 -> isAlign
                                2 -> isGeekAlign
                                else -> 0
                            }

                            textV.textAlignment = when (alignment) {
                                1 -> View.TEXT_ALIGNMENT_CENTER
                                2 -> View.TEXT_ALIGNMENT_TEXT_END
                                else -> View.TEXT_ALIGNMENT_TEXT_START
                            }

                            // 双排时钟行间距调整
                            if ((getMode == 1 && isClockDouble) || getMode == 2) {
                                textLineSpacing(textV)
                            }
                        }
                    }
                }
            }
    }

    private fun margin(id: TextView) {
        // 上下偏移量
        var topMargin = 0
        if (verticalOffset != 12) {
            topMargin = dp2px((verticalOffset - 12) * 0.5f)
        }
        id.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)
    }

    private fun textLineSpacing(id: TextView) {
        when {
            lineSpacing != 16 && getMode == 1 -> {
                id.setLineSpacing(0f, lineSpacing * 0.05f)
            }
            lineSpacingGeek != 16 && getMode == 2 -> {
                id.setLineSpacing(0f, lineSpacingGeek * 0.05f)
            }
        }
    }
}
