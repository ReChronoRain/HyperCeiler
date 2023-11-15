package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.invokeMethod
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.view.WeatherData


@SuppressLint("StaticFieldLeak")
object NotificationWeatherNew : BaseHook() {
    lateinit var weather: WeatherData
    var clockId: Int = -2

    @SuppressLint("DiscouragedApi", "ClickableViewAccessibility")
    override fun init() {
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
            if (isAndroidVersion(34)) "com.android.systemui.shared.plugins.PluginInstance\$Factory\$\$ExternalSyntheticLambda0"
            else if (isAndroidVersion(33)) "com.android.systemui.shared.plugins.PluginInstance\$Factory"
            else "com.android.systemui.shared.plugins.PluginManagerImpl"

        var appInfo: ApplicationInfo?

        if (isAndroidVersion(34) && !isMoreHyperOSVersion(1f)) {
            hookAllMethods("com.android.systemui.shared.plugins.PluginInstance\$Factory",
                "create", object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        appInfo = param.args[1] as ApplicationInfo

                        loadClass(pluginLoaderClass, lpparam.classLoader).methodFinder().first {
                            name == "get"
                        }.createHook {
                            after { getClassLoader ->
                                val classLoader = getClassLoader.result as ClassLoader
                                if (appInfo != null) {
                                    if ("miui.systemui.plugin" == appInfo!!.packageName) {
                                        mainPanelHeader(classLoader)
                                        logW(TAG, "com.android.systemui", "im get ClassLoader: $classLoader")
                                    } else {
                                        logW(TAG, "com.android.systemui", "Au get classloader miui.systemui.plugin error: $classLoader")
                                    }
                                } else {
                                    if (
                                        classLoader.toString().contains("MIUISystemUIPlugin") ||
                                        classLoader.toString().contains("miui.systemui.plugin")
                                    ) {
                                        mainPanelHeader(classLoader)
                                    } else {
                                        logW(TAG, "com.android.systemui", "Au get classloader miui.systemui.plugin error & appInfo is null")
                                    }
                                }
                            }
                        }
                    }
                }
            )
        } else {
            loadClass(pluginLoaderClass, lpparam.classLoader).methodFinder().first {
                name == "getClassLoader"
            }.createHook {
                after { getClassLoader ->
                    appInfo = getClassLoader.args[0] as ApplicationInfo
                    if (appInfo!!.packageName == "miui.systemui.plugin") {
                        val classLoader = getClassLoader.result as ClassLoader
                        mainPanelHeader(classLoader)
                    }
                }
            }
        }
    }

    fun mainPanelHeader(pluginClassLoader: ClassLoader) {
        if (isMoreHyperOSVersion(1f)) return
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        loadClass(
            "miui.systemui.controlcenter.windowview.MainPanelHeaderController",
            pluginClassLoader
        ).methodFinder().first {
            name == "addClockViews"
        }.createHook {
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
