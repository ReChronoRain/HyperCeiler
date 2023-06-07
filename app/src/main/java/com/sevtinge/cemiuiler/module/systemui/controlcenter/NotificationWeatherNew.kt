package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectFieldAs
import com.sevtinge.cemiuiler.view.WeatherData


@SuppressLint("StaticFieldLeak")
object NotificationWeatherNew : BaseHook() {
    // TODO: Android13控制中心天气不可用
    lateinit var weather: WeatherData
    var clockId: Int = -2

    @SuppressLint("DiscouragedApi", "ClickableViewAccessibility")
    override fun init() {
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        val mControlCenterDateViewClass =
            loadClass("com.android.systemui.controlcenter.phone.widget.ControlCenterDateView")

        mControlCenterDateViewClass.methodFinder().findSuper().first {
            name == "onDetachedFromWindow"
        }.createHook {
            before {
                if ((it.thisObject as TextView).id == clockId /* && this::weather.isInitialized */) {
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
                if (view.id == clockId && time != null /* && this::weather.isInitialized */) {
//                val layout = view.layoutParams as ViewGroup.MarginLayoutParams
//                val y = view.height / 2
//                layout.topMargin = -y
                    it.args[0] = "${weather.weatherData}$time"
                }
            }
        }

        loadClass("com.android.systemui.shared.plugins.PluginManagerImpl").methodFinder().first {
            name == "getClassLoader"
        }.createHook {
            after { getClassLoader ->
                val appInfo = getClassLoader.args[0] as ApplicationInfo
                val classLoader = getClassLoader.result as ClassLoader
                if (appInfo.packageName == "miui.systemui.plugin") {
                    loadClass(
                        "miui.systemui.controlcenter.windowview.MainPanelHeaderController",
                        classLoader
                    ).methodFinder().first {
                        name == "addClockViews"
                    }.createHook {
                        after {
                            val dateView = it.thisObject.getObjectFieldAs<TextView>("dateView")
                            clockId = dateView.id
                            weather = WeatherData(dateView.context, isDisplayCity)
                            weather.callBacks = {
                                dateView.callMethod("updateTime")
                            }
                        }
                    }
                }
            }
        }

    }

}
