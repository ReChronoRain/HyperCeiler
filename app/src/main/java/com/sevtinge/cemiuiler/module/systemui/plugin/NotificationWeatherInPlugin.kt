package com.sevtinge.cemiuiler.module.systemui.plugin

import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.systemui.controlcenter.NotificationWeatherNew
import com.sevtinge.cemiuiler.utils.getObjectFieldAs
import com.sevtinge.cemiuiler.view.WeatherData
import de.robv.android.xposed.XposedHelpers

object NotificationWeatherInPlugin {
    @JvmStatic
    fun initNotificationWeatherInPlugin(classLoader: ClassLoader?) {
        val isDisplayCity =
            BaseHook.mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        XposedHelpers.findAndHookMethod(
            "miui.systemui.controlcenter.windowview.MainPanelHeaderController",
            classLoader,
            "addClockViews",
            object : BaseHook.MethodHook() {
                override fun after(param: MethodHookParam) {
                    val dateView = param.thisObject.getObjectFieldAs<TextView>("dateView")
                    NotificationWeatherNew.clockId = dateView.id
                    NotificationWeatherNew.weather = WeatherData(dateView.context, isDisplayCity)
                    //NotificationWeatherNew.weather.callBacks = { dateView.callMethod("updateTime") }
                }
            })
    }
}
