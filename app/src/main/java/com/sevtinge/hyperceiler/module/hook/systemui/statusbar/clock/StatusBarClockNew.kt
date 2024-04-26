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

import android.content.*
import android.graphics.*
import android.os.*
import android.util.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.mNewClockClass
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import java.lang.reflect.*
import java.util.*

object StatusBarClockNew : BaseHook() {
    private val statusBarClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiClock")
    }

    private val clockBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_bold")
    }
    private val isBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_big_bold")
    }
    private val isSync by lazy {
        mPrefsMap.getBoolean("system_ui_disable_clock_synch")
    }
    private val clockSizeS by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_1", 12)
    }
    private val clockSizeB by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_2", 54)
    }
    private val clockSizeN by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_3", 12)
    }
    private val clockTextSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_double_spacing_margin_1", 16)
    }
    private val sClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_1", 0)
    }
    private val sClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_1", 0)
    }
    private val sClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_1", 12)
    }
    private val fixedWidth by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_fixedcontent_width_1", 30)
    }
    private val bClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_2", 0)
    }
    private val bClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_2", 0)
    }
    private val bClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_2", 12)
    }
    private val nClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_3", 0)
    }
    private val nClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_3", 0)
    }
    private val nClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_3", 12)
    }
    private val clockAlign by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_1", 0)
    }
    private val isShowSec by lazy {
        mPrefsMap.getBoolean("system_ui_clock_is_show_sec")
    }

    // 时钟格式
    private val getFormatS = mPrefsMap.getString("system_ui_statusbar_clock_editor_s", "HH:mm:ss")
    private val getFormatN = mPrefsMap.getString("system_ui_statusbar_clock_editor_n", "")
    private val getClockStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_style", 0)
    }

    override fun init() {
        statusBarClass.constructorFinder()
            .filterByParamCount(3)
            .first().createAfterHook {
                try {
                    val miuiClock = it.thisObject as TextView
                    val miuiClockName = miuiClock.resources.getResourceEntryName(miuiClock.id)
                        ?: return@createAfterHook

                    setMiuiClockStyle(miuiClockName, miuiClock)

                    val isSec =
                        miuiClockName in setOf("clock", "big_time", "horizontal_time", "date_time")
                    // miuiClockName 内部标签分类如下
                    // clock 竖屏状态栏时钟
                    // big_time 通知中心时钟
                    // horizontal_time 横屏通知中心时钟
                    // date_time 通知中心日期时钟

                    if (getClockStyle != 0 && miuiClockName == "clock")
                        miuiClock.isSingleLine = false

                    if (isSec && isShowSec) {
                        val d: Method = miuiClock.javaClass.getDeclaredMethod("updateTime")
                        val r = Runnable {
                            d.isAccessible = true
                            d.invoke(miuiClock)
                        }

                        class T : TimerTask() {
                            override fun run() {
                                Handler(miuiClock.context.mainLooper).post(r)
                            }
                        }
                        Timer().schedule(
                            T(), 1000 - System.currentTimeMillis() % 1000, 1000
                        )
                    }
                } catch (_: Exception) {
                }
            }

        // 设置格式
        statusBarClass.methodFinder()
            .filterByName("updateTime")
            .single().createBeforeHook {
                try {
                    val textV = it.thisObject as TextView
                    val context = textV.context
                    val miuiClockName =
                        textV.resources.getResourceEntryName(textV.id) ?: return@createBeforeHook
                    if (miuiClockName in setOf("clock", "big_time", "horizontal_time", "date_time")) {
                        setMiuiClockStyle(miuiClockName, textV)

                        if ((isSync && miuiClockName == "big_time") || (getFormatN.isEmpty() && miuiClockName in setOf("date_time", "horizontal_time"))) return@createBeforeHook
                        setMiuiClockFormat(context, miuiClockName, textV)
                        it.result = null
                    }
                } catch (_: Exception) {
                }
            }

        mNewClockClass.methodFinder()
            .filterByName("updateTime")
            .single().createBeforeHook {
                try {
                    val textV = it.thisObject as TextView
                    val context = textV.context
                    val miuiClockName =
                        textV.resources.getResourceEntryName(textV.id) ?: return@createBeforeHook
                    setMiuiClockStyle(miuiClockName, textV)

                    if ((isSync && miuiClockName == "big_time") || (getFormatN.isEmpty() && miuiClockName in setOf("date_time", "horizontal_time"))) return@createBeforeHook
                    setMiuiClockFormat(context, miuiClockName, textV)
                    it.result = null
                } catch (_: Exception) {
                }
            }
    }

    private fun setMiuiClockStyle(name: String, text: TextView) {
        // 时钟加粗
        if (clockBold && (name == "clock" || (name == "big_time" && isBold))) {
            text.typeface = Typeface.DEFAULT_BOLD
        }

        // 时钟大小
        setStatusBarClock(name, text)

        if (getClockStyle != 0 && name == "clock") {
            // 状态栏时钟双排对齐
            text.textAlignment = when (clockAlign) {
                1 -> View.TEXT_ALIGNMENT_CENTER
                2 -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            // 设置双排时钟行间距
            text.setLineSpacing(0f, clockTextSpacing * 0.05f)
        }

        // 设置时钟边距
        if (name == "clock") {
            setClockMargin(text, sClockLeftMargin, sClockRightMargin, sClockVerticalOffset)

            // 固定宽度
            if (fixedWidth > 30) {
                text.width = (text.resources.displayMetrics.density * fixedWidth).toInt()
            }
        } else if (name == "big_time") {
            setClockMargin(text, bClockLeftMargin, bClockRightMargin, bClockVerticalOffset)
        } else {
            setClockMargin(text, nClockLeftMargin, nClockRightMargin, nClockVerticalOffset)
        }
    }

    private fun setStatusBarClock(name: String, text: TextView) {
        when {
            clockSizeS != 12 && name == "clock" -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeS.toFloat())
            }

            clockSizeB != 54 && name == "big_time" -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeB.toFloat())
            }

            clockSizeN != 12 && name in setOf("date_time", "horizontal_time") -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeN.toFloat())
            }
        }
    }

    private fun setClockMargin(
        id: TextView,
        leftMargin: Int,
        rightMargin: Int,
        verticalOffset: Int
    ) {
        val left = dp2px(leftMargin.toFloat())
        val right = dp2px(rightMargin.toFloat())
        var topMargin = 0
        if (verticalOffset != 12) {
            topMargin = dp2px((verticalOffset - 12) * 0.5f)
        }
        id.setPaddingRelative(left, topMargin, right, 0)
    }

    private fun setMiuiClockFormat(context: Context?, name: String, textV: TextView) {
        val textSb: StringBuilder
        val formatSb: StringBuilder

        // 因为输入对话框限制，所以里面部分内容会比较抽象
        val sClockName = if (getFormatN.isEmpty()) {
            when (getClockStyle) {
                0 -> getFormatS.split("\n")[0]
                1 -> "${getFormatS.split("\n")[0]}\nM/d E"
                else -> "M/d E\n${getFormatS.split("\n")[0]}"
            }
        } else {
            when (getClockStyle) {
                0 -> getFormatS.split("\n")[0]
                1 -> "${getFormatS.split("\n")[0]}\n${getFormatN.split("\n")[0]}"
                else -> "${getFormatN.split("\n")[0]}\n${getFormatS.split("\n")[0]}"
            }
        }

        val mMiuiStatusBarClockController =
            textV.getObjectField("mMiuiStatusBarClockController")
        val mCalendar =
            if (isAndroidVersion(34)) {
                mMiuiStatusBarClockController?.getObjectField("mCalendar")
            } else {
                mMiuiStatusBarClockController?.callMethod("getCalendar")
            }
        mCalendar?.callMethod("setTimeInMillis", System.currentTimeMillis())

        when (name) {
            "clock" -> {
                textSb = StringBuilder()
                formatSb = StringBuilder(sClockName)
            }

            "big_time" -> {
                textSb = StringBuilder()
                formatSb = StringBuilder(getFormatS.split("\n")[0])
            }

            "horizontal_time" -> {
                textSb = StringBuilder()
                formatSb =
                    StringBuilder("${getFormatN.split("\n")[0]} ${getFormatS.split("\n")[0]}")
            }

            else -> {
                textSb = StringBuilder()
                formatSb = StringBuilder(getFormatN.split("\n")[0])
            }
        }
        mCalendar?.callMethod("format", context, textSb, formatSb)
        textV.text = textSb.toString()
    }
}