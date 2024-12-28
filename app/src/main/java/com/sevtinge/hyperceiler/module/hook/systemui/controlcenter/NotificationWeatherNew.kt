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
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.invokeMethod
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.view.WeatherData


@SuppressLint("StaticFieldLeak")
// 控制中心添加天气信息
object NotificationWeatherNew : BaseHook() {
    lateinit var weather: WeatherData
    private var clockId: Int = -2

    @SuppressLint("DiscouragedApi", "ClickableViewAccessibility")
    override fun init() {
        val mControlCenterDateViewClass =
            loadClass("com.android.systemui.controlcenter.phone.widget.ControlCenterDateView")

        mControlCenterDateViewClass.methodFinder().findSuper()
            .filterByName("onDetachedFromWindow")
            .first().createHook {
                before {
                    if ((it.thisObject as TextView).id == clockId && this@NotificationWeatherNew::weather.isInitialized) {
                        weather.onDetachedFromWindow()
                    }
                }
            }
        mControlCenterDateViewClass.methodFinder().findSuper()
            .filterByName("setText")
            .first().createHook {
                before {
                    val time = it.args[0]?.toString()
                    val view = it.thisObject as TextView
                    if (view.id == clockId && time != null && this@NotificationWeatherNew::weather.isInitialized) {
                        // val layout = view.layoutParams as ViewGroup.MarginLayoutParams
                        // val y = view.height / 2
                        // layout.topMargin = -y
                        it.args[0] = "${weather.weatherData}$time"
                    }
                }
            }
    }

    @JvmStatic
    fun mainPanelHeader(pluginClassLoader: ClassLoader) {
        if (isMoreHyperOSVersion(1f)) return
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        loadClass(
            "miui.systemui.controlcenter.windowview.MainPanelHeaderController",
            pluginClassLoader
        ).methodFinder().filterByName("addClockViews").first().createHook {
            after {
                val dateView =
                    it.thisObject.getObjectFieldOrNullAs<TextView>("dateView")!!
                clockId = dateView.id
                weather = WeatherData(dateView.context, isDisplayCity)
                weather.callBacks = {
                    dateView.invokeMethod("updateTime")
                }
            }
        }
    }
}
