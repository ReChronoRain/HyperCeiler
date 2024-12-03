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

import android.annotation.*
import android.content.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.view.*

@SuppressLint("DiscouragedApi", "ServiceCast", "StaticFieldLeak")
object NotificationWeather : BaseHook() {
    private var mWeatherView : TextView? = null
    private var mWeatherViewFolme : Any? = null
    private val isDisplayCity by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
    }

    override fun init() {
        if (isMoreHyperOSVersion(2f)) {
            newNotificationWeather()
        } else {
            oldNotificationWeather()
        }

        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("updateLayout")
            .single().createHook {
                after {
                    val viewGroup = it.thisObject as ViewGroup
                    val mOrientation = viewGroup.getObjectField("mOrientation") as Int

                    mWeatherView?.visibility = if (mOrientation == 1) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
    }

    private fun newNotificationWeather() {
        loadClass("com.android.systemui.controlcenter.shade.CombinedHeaderController").constructors
            .single().createHook {
                after { param ->
                    val headerController = param.thisObject
                    val notificationDateTime = headerController.getObjectFieldAs<View>("notificationDateTime")
                    val context = notificationDateTime.context

                    addWeatherViewAfterOf(notificationDateTime)
                    setWeatherViewOnClinkListener(context)

                    mWeatherView?.let {
                        mWeatherViewFolme = loadClass("miuix.animation.Folme")
                            .callStaticMethod("useAt", arrayOf<View>(it))
                    }
                }
            }

        loadClass("com.android.systemui.controlcenter.shade.CombinedHeaderController").methodFinder()
            .filterByName("onSwitchProgressChanged")
            .filterByParamTypes(Float::class.java)
            .first().createHook {
                after { param ->
                    val headerController = param.thisObject
                    val dateTime = headerController.getObjectFieldAs<View>("notificationDateTime")
                    mWeatherView?.translationX = dateTime.translationX
                    mWeatherView?.translationY = dateTime.translationY
                }
            }

        val expandControllerClz = loadClass(
            "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController"
        )
        expandControllerClz.constructors
            .single().createHook {
                after { param ->
                    val expandController = param.thisObject
                    expandController.getObjectFieldAs<Any>("notificationCallback")::class.java
                        .methodFinder()
                        .filterByName("onExpansionChanged")
                        .first()
                        .createHook {
                            after {
                                val dateTime = expandController.getObjectFieldAs<Any>("headerController")
                                    .callMethodAs<Any>("get")
                                    .getObjectFieldAs<View>("notificationDateTime")

                                mWeatherView?.translationX = dateTime.translationX
                                mWeatherView?.translationY = dateTime.translationY
                            }
                        }
                }
            }
        expandControllerClz.methodFinder()
            .filterByName("access\$startFolmeAnimationAlpha")
            .filterByParamCount(5)
            .single().createHook {
                before { param ->
                    val view = param.args[1] as View
                    val context = view.context
                    val dateTimeId = context.resources.getIdentifier("date_time", "id", context.packageName)
                    if (view.id == dateTimeId) {
                        expandControllerClz.callStaticMethod(
                            "access\$startFolmeAnimationAlpha",
                            param.args[0],
                            mWeatherView,
                            mWeatherViewFolme,
                            param.args[3],
                            param.args[4],
                        )
                    }
                }
            }

        expandControllerClz.methodFinder()
            .filterByName("access\$startFolmeAnimationTranslationX")
            .single().createHook {
                before { param ->
                    val view = param.args[1] as View
                    val context = view.context
                    val dateTimeId = context.resources.getIdentifier("date_time", "id", context.packageName)
                    if (view.id == dateTimeId) {
                        expandControllerClz.callStaticMethod(
                            "access\$startFolmeAnimationTranslationX",
                            param.args[0],
                            mWeatherView,
                            mWeatherViewFolme,
                            param.args[3],
                            param.args[4],
                        )
                    }
                }
            }
    }

    private fun oldNotificationWeather() {
        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("onFinishInflate")
            .single().createHook {
                after { param ->
                    val viewGroup = param.thisObject as ViewGroup
                    val context = viewGroup.context

                    val dateTime = viewGroup.findViewById<View>(
                        context.resources.getIdentifier(
                            "date_time",
                            "id",
                            context.packageName
                        )
                    )

                    addWeatherViewAfterOf(dateTime)
                    setWeatherViewOnClinkListener(context)
                }
            }
    }

    private fun addWeatherViewAfterOf(dateTimeView : View) {
        mWeatherView = WeatherView(dateTimeView.context, isDisplayCity).apply {
            setTextAppearance(
                context.resources.getIdentifier(
                    "TextAppearance.QSControl.Date",
                    "style",
                    context.packageName
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                marginStart = context.resources.getDimensionPixelSize(
                    context.resources.getIdentifier(
                        "notification_panel_time_date_space",
                        "dimen",
                        context.packageName
                    )
                ) + dp2px(5f)
            }
        }

        val dateTimeParent = dateTimeView.parent as ViewGroup
        dateTimeParent.addView(mWeatherView)
    }

    private fun setWeatherViewOnClinkListener(context : Context) {
        mWeatherView?.setOnClickListener {
            try {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    component = ComponentName(
                        "com.miui.weather2",
                        "com.miui.weather2.ActivityWeatherMain"
                    )
                }

                val clz = findClass(InterfacesImplManager.I_ACTIVITY_STARTER)
                if (isMoreHyperOSVersion(2f)) {
                    InterfacesImplManager.sClassContainer[clz]
                } else {
                    Dependency.get(clz)
                }?.callMethod("startActivity", intent, true)
            } catch (e : Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
