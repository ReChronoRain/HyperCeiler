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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

import com.sevtinge.hyperceiler.utils.setObjectField
import com.sevtinge.hyperceiler.view.WeatherView

// 经典控制中心添加天气信息
object OldWeather : BaseHook() {
    private val isDisplayCity by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
    }

    @SuppressLint("DiscouragedApi")
    override fun init() {
        var mWeatherView: TextView?
        loadClass("com.android.systemui.qs.MiuiQSHeaderView").methodFinder()
            .filterByName("onFinishInflate")
            .first().createHook {
                after {
                    val viewGroup = it.thisObject as ViewGroup
                    val context = viewGroup.context
                    val layoutParam =
                        loadClass("androidx.constraintlayout.widget.ConstraintLayout\$LayoutParams")
                            .getConstructor(Int::class.java, Int::class.java)
                            .newInstance(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ) as ViewGroup.MarginLayoutParams

                    layoutParam.setObjectField(
                        "endToStart",
                        context.resources.getIdentifier(
                            "notification_shade_shortcut",
                            "id",
                            context.packageName
                        )
                    )
                    layoutParam.setObjectField(
                        "topToTop",
                        context.resources.getIdentifier(
                            "notification_shade_shortcut",
                            "id",
                            context.packageName
                        )
                    )
                    layoutParam.setObjectField(
                        "bottomToBottom",
                        context.resources.getIdentifier(
                            "notification_shade_shortcut",
                            "id",
                            context.packageName
                        )
                    )

                    mWeatherView = WeatherView(context, isDisplayCity).apply {
                        setTextAppearance(
                            context.resources.getIdentifier(
                                "TextAppearance.StatusBar.Expanded.Clock.QuickSettingDate",
                                "style",
                                context.packageName
                            )
                        )
                        layoutParams = layoutParam

                        setOnClickListener {
                            startWeatherApp()
                        }
                    }
                    viewGroup.addView(mWeatherView)
                }
            }
    }

}
