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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.getObjectField

object NewNetworkSpeedStyle : BaseHook() {
    private val viewInitedTag = getFakeResId("view_inited_tag")

    private val fixedWidth by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10)
    }
    private val networkStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_style", 0)
    }

    private val lineSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_spacing_margin", 16)
    }
    private val bold by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_font_style", 0)
    }
    private val fontSize by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13)
    }
    private val fontSizeEnable by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_font_size_enable")
    }
    private val align by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1)
    }

    override fun init() {
        hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader,
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val meter = param.thisObject as View
                    val inited = meter.getTag(viewInitedTag)
                    if (inited == null && "slot_text_icon" != meter.tag) {
                        meter.setTag(viewInitedTag, true)

                        if (fixedWidth > 10) {
                            var lp = meter.layoutParams
                            val viewWidth =
                                (meter.resources.displayMetrics.density * fixedWidth).toInt()
                            if (lp == null) {
                                lp = ViewGroup.LayoutParams(viewWidth, -1)
                            } else {
                                lp.width = viewWidth
                            }
                            meter.layoutParams = lp
                        }

                        meter.postDelayed( {
                            val number = meter.getObjectField("mNetworkSpeedNumberText") as? TextView ?: return@postDelayed
                            val unit = meter.getObjectField("mNetworkSpeedUnitText") as? TextView ?: return@postDelayed

                            if (networkStyle != 0) {
                                unit.visibility = View.GONE
                                if (networkStyle == 2 || networkStyle == 4) {
                                    number.isSingleLine = false
                                    number.maxLines = 2
                                }
                                textFont(number) // 加粗
                                margin(number) // 偏移量设置
                                align(number) // 水平对齐
                                textSize(number) // 网速字体大小调整
                                textLineSpacing(number) // 网速行间距调整
                            } else {
                                // 加粗
                                textFont(number)
                                textFont(unit)
                                // 偏移量设置
                                margin(number)
                                margin(unit)
                                // 水平对齐官方的寄，改不了
                                textSize(number) // 网速字体大小调整
                                textSize(unit) // 网速字体大小调整
                            }
                        }, 150)
                    }
                }
            })
    }

    private fun textLineSpacing(id: TextView) {
        if (networkStyle == 2 || networkStyle == 4) {
            id.setLineSpacing(0f, lineSpacing * 0.05f)
        }
    }

    private fun textFont(id: TextView) {
        when (bold) {
            1 -> id.typeface = Typeface.DEFAULT
            2 -> id.typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun textSize(id: TextView) {
        if (fontSizeEnable) {
            val setSize = when (networkStyle) {
                0, 2, 4 -> fontSize * 0.5f
                else -> fontSize.toFloat()
            }

            id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, setSize)
        }
    }

    private fun align(id: TextView) {
        when (align) {
            2 -> id.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            3 -> id.textAlignment = View.TEXT_ALIGNMENT_CENTER
            4 -> id.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        }
    }

    private fun margin(id: TextView) {
        val leftMargin = dp2px(
            mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0) * 0.5f
        )
        val rightMargin = dp2px(
            mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0) * 0.5f
        )
        val verticalOffset = mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 40)
        val topMargin = if (verticalOffset != 40) {
            dp2px((verticalOffset - 40) * 0.1f)
        } else {
            0
        }
        id.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)
    }
}
