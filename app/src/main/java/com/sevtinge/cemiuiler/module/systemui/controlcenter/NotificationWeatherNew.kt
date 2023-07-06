package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.annotation.SuppressLint
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectFieldOrNullAs
import com.sevtinge.cemiuiler.view.WeatherData


@SuppressLint("StaticFieldLeak")
object NotificationWeatherNew : BaseHook() {
    // TODO: Android13 控制中心天气不可用
    lateinit var weather: WeatherData
    var clockId: Int = -2

    @SuppressLint("DiscouragedApi", "ClickableViewAccessibility")
    override fun init() {
        val mControlCenterDateViewClass =
            loadClass("com.android.systemui.controlcenter.phone.widget.QSControlCenterHeaderView")

        mControlCenterDateViewClass.methodFinder().findSuper().first {
            name == "onDetachedFromWindow"
        }.createHook {
            before {
                if ((it.thisObject as TextView).id == clockId && this@NotificationWeatherNew::weather.isInitialized) {
                    weather.onDetachedFromWindow()
                }
            }
        }
        mControlCenterDateViewClass.methodFinder().findSuper().first {
            name == "setText"
        }.createHook {
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
    fun notificationWeatherInPlugin(classLoader: ClassLoader?) {
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        loadClass(
            "miui.systemui.controlcenter.windowview.MainPanelHeaderController", classLoader
        ).methodFinder().first {
            name == "addClockViews"
        }.createHook {
            after {
                val dateView = it.thisObject.getObjectFieldOrNullAs<TextView>("dateView")!!
                clockId = dateView.id
                weather = WeatherData(dateView.context, isDisplayCity)
                weather.callBacks = {
                    dateView.callMethod("updateTime")
                }
            }
        }
    }
}
