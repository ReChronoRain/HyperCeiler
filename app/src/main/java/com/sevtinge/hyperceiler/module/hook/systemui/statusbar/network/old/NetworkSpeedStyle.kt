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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import de.robv.android.xposed.XposedHelpers

object NetworkSpeedStyle : BaseHook() {
    private val fontSize by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13)
    }
    private val fontSizeEnable by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_font_size_enable")
    }
    private val lineSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_spacing_margin", 16)
    }
    private val bold by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_font_style", 0)
    }
    private val align by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1)
    }
    private val networkStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_style", 0)
    }

    override fun init() {
        if (isAndroidVersion(30)) {
            // Android 11 or MIUI12.5 Need to hook Statusbar in Screen Lock interface, to set front size
            // Thanks for CustoMIUIzerMod
            loadClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView").methodFinder().first {
                name == "onDensityOrFontScaleChanged"
            }.createHook {
               after { params ->
                   val meter = XposedHelpers.getObjectField(params.thisObject, "mNetworkSpeedView") as TextView

                   // 网速字体大小调整
                   textSize(meter)

                   // 网速行间距调整
                   textLineSpacing(meter)
               }
            }
        }

        hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    // 值和单位双排显示 + 上下行网速双排显示
                    val meter = param.thisObject as TextView

                    if (meter.tag == null || "slot_text_icon" != meter.tag) {
                        // 网速加粗
                        when (bold) {
                            1 -> meter.typeface = Typeface.DEFAULT
                            2 -> meter.typeface = Typeface.DEFAULT_BOLD
                        }


                        // 左侧间距
                        var leftMargin =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0)
                        leftMargin = dp2px(leftMargin * 0.5f)

                        // 右侧间距
                        var rightMargin =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0)
                        rightMargin = dp2px(rightMargin * 0.5f)

                        // 上下偏移量
                        var topMargin = 0
                        val verticalOffset =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 8)
                        if (verticalOffset != 8) {
                            topMargin = dp2px((verticalOffset - 8) * 0.5f)
                        }
                        meter.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

                        // 网速字体大小调整
                        textSize(meter)

                        // 网速行间距调整
                        textLineSpacing(meter)

                        // 水平对齐
                        when (align) {
                            2 -> meter.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                            3 -> meter.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            4 -> meter.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                        }
                    }
                }
            }
        )
    }

    private fun textLineSpacing(id: TextView) {
        if (lineSpacing != 16 && (networkStyle == 2 || networkStyle == 4)) {
            id.setLineSpacing(0f, lineSpacing * 0.05f)
        }
    }

    private fun textSize(id: TextView) {
        if (fontSizeEnable) {
            try {
                if (networkStyle == 2 || networkStyle == 4) {
                    id.isSingleLine = false
                    id.maxLines = 2
                    id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                } else {
                    id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize.toFloat())
                }
            } catch (e: Exception) {
                logE(TAG, this@NetworkSpeedStyle.lpparam.packageName, e)
            }
        }
    }
}
