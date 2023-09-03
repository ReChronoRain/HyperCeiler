package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.invokeMethod
import com.sevtinge.cemiuiler.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.cemiuiler.utils.getObjectFieldOrNullAs
import com.sevtinge.cemiuiler.view.WeatherData


@SuppressLint("StaticFieldLeak")
object NotificationWeatherNew : BaseHook() {
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

        val pluginLoaderClass =
            if (isMoreAndroidVersion(33)) "com.android.systemui.shared.plugins.PluginInstance\$Factory"
            else "com.android.systemui.shared.plugins.PluginManagerImpl"

        loadClass(pluginLoaderClass, lpparam.classLoader).methodFinder().first {
            name == "getClassLoader"
        }.createHook {
            after { getClassLoader ->
                val appInfo = getClassLoader.args[0] as ApplicationInfo
                if (appInfo.packageName == "miui.systemui.plugin") {
                    val classLoader = getClassLoader.result as ClassLoader
                    loadClass(
                        "miui.systemui.controlcenter.windowview.MainPanelHeaderController",
                        classLoader
                    ).methodFinder().first {
                        name == "addClockViews"
                    }.createHook {
                        after {
                            val dateView = it.thisObject.getObjectFieldOrNullAs<TextView>("dateView")!!
                            clockId = dateView.id
                            weather = WeatherData(dateView.context, isDisplayCity)
                            weather.callBacks = {
                                dateView.invokeMethod("updateTime")
                            }
                        }
                    }
                }
            }
        }
    }
}
