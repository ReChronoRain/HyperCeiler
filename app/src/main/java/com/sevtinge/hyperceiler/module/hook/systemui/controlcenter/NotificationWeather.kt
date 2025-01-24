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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.*
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.*
import android.widget.*
import androidx.annotation.IntDef
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.miuiConfigs
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.view.*
import de.robv.android.xposed.XposedHelpers.findMethodExactIfExists

@SuppressLint("DiscouragedApi", "ServiceCast", "StaticFieldLeak")
object NotificationWeather : BaseHook() {
    // 横屏状态下的天气组件
    private var hWeatherView: TextView? = null
    // 及动画
    private var hWeatherViewFolme: Any? = null

    // 竖屏状态下的天气组件
    private var vWeatherView: TextView? = null
    // 及动画
    private var vWeatherViewFolme: Any? = null

    // 是否显示城市
    private val isDisplayCity by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
    }

    private val combinedHeaderController by lazy {
        loadClass("com.android.systemui.controlcenter.shade.CombinedHeaderController")
    }
    private val notificationHeaderExpandController by lazy {
        loadClass("com.android.systemui.controlcenter.shade.NotificationHeaderExpandController")
    }
    private val miuiNotificationHeaderView by lazy {
        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView")
    }
    private val folme by lazy {
        loadClass("miuix.animation.Folme")
    }

    override fun init() {
        if (isMoreHyperOSVersion(2f)) {
            newNotificationWeather()
        } else {
            oldNotificationWeather()
        }

        // 更新资源
        updateResources()
        // 更新布局
        updateLayout()
    }

    private fun updateResources() {
        var method = findMethodExactIfExists(miuiNotificationHeaderView, "updateHeaderResources")
        if (method == null) {
            method = findMethodExactIfExists(miuiNotificationHeaderView, "updateResources")
        }

        method?.createAfterHook { param ->
            val viewGroup = param.thisObject as ViewGroup
            val orientation = viewGroup.getObjectFieldAs<Int>("mOrientation")
            if (orientation == -1) {
                return@createAfterHook
            }

            val dateView = viewGroup.getObjectFieldAs<TextView>("mDateView")
            val landClock = viewGroup.getObjectFieldAs<TextView>("mLandClock")

            vWeatherView?.setTextSize(0, dateView.textSize)
            vWeatherView?.typeface = dateView.typeface

            hWeatherView?.setTextSize(0, landClock.textSize)
            hWeatherView?.typeface = landClock.typeface
        }
    }

    private fun updateLayout() {
        miuiNotificationHeaderView.methodFinder()
            .filterByName("updateLayout")
            .single().createBeforeHook {
                val viewGroup = it.thisObject as ViewGroup
                val context = viewGroup.context
                val configuration = context.resources.configuration
                var orientation = viewGroup.getObjectFieldAs<Int>("mOrientation")
                val screenLayout = viewGroup.getObjectFieldAs<Int>("mScreenLayout")

                if (orientation == configuration.orientation &&
                    screenLayout == configuration.screenLayout
                ) {
                    return@createBeforeHook
                }

                orientation = configuration.orientation

                val isVerticalMode = if (isMoreHyperOSVersion(2f)) {
                    miuiConfigs.callStaticMethodAs("isVerticalMode", context)
                } else {
                    orientation == ORIENTATION_PORTRAIT || isLargeUI()
                }

                if (isVerticalMode) {
                    hWeatherView?.visibility = View.GONE
                    vWeatherView?.visibility = View.VISIBLE
                } else {
                    hWeatherView?.visibility = View.VISIBLE
                    vWeatherView?.visibility = View.GONE
                }
            }
    }

    private fun newNotificationWeather() {
        combinedHeaderController.constructors.single().createAfterHook { param ->
            val controller = param.thisObject
            val dateView = controller.getObjectFieldAs<View>("notificationDateTime")
            val landClock = controller.getObjectFieldAs<View>("notificationHorizontalTime")

            addWeatherViewAfterOf(dateView, ORIENTATION_PORTRAIT)
            addWeatherViewAfterOf(landClock, ORIENTATION_LANDSCAPE)

            // 创建动画
            hWeatherView?.let {
                hWeatherViewFolme = folme.callStaticMethod("useAt", arrayOf<View>(it))
            }
            vWeatherView?.let {
                vWeatherViewFolme = folme.callStaticMethod("useAt", arrayOf<View>(it))
            }
        }

        combinedHeaderController.methodFinder()
            .filterByName("onSwitchProgressChanged")
            .filterByParamTypes(Float::class.java)
            .first().createAfterHook { param ->
                val controller = param.thisObject
                val dateView = controller.getObjectFieldAs<View>("notificationDateTime")
                val landClock = controller.getObjectFieldAs<View>("notificationHorizontalTime")

                vWeatherView?.translationX = dateView.translationX
                vWeatherView?.translationY = dateView.translationY

                hWeatherView?.translationX = landClock.translationX
                hWeatherView?.translationY = landClock.translationY
            }

        notificationHeaderExpandController.constructors.single().createAfterHook { param ->
            val expandController = param.thisObject
            val callback = expandController.getObjectFieldAs<Any>("notificationCallback")

            hookNotificationCallback(expandController, callback::class.java)
        }
    }

    private fun hookNotificationCallback(expandController: Any, clazz: Class<*>) {
        clazz.methodFinder().filterByName("onAppearanceChanged").first().createAfterHook {
            val newAppearance = it.args[0] as Boolean
            val animate = it.args[1] as Boolean

            val startFolmeAnimationAlpha = { view: View?, folme: Any? ->
                notificationHeaderExpandController.callStaticMethod(
                    "access\$startFolmeAnimationAlpha",
                    expandController,
                    view,
                    folme,
                    if (newAppearance) 1F else 0F,
                    animate,
                )
            }

            startFolmeAnimationAlpha(hWeatherView, hWeatherViewFolme)
            startFolmeAnimationAlpha(vWeatherView, vWeatherViewFolme)
        }

        clazz.methodFinder().filterByName("onExpansionChanged").first().createAfterHook {
            val headerController = expandController.getObjectFieldAs<Any>("headerController")
                .callMethodAs<Any>("get")

            headerController.getObjectFieldAs<View>("notificationDateTime").let {
                vWeatherView?.translationX = it.translationX
                vWeatherView?.translationY = it.translationY
            }

            headerController.getObjectFieldAs<View>("notificationHorizontalTime").let {
                hWeatherView?.translationX = it.translationX
                hWeatherView?.translationY = it.translationY
            }
        }
    }

    private fun oldNotificationWeather() {
        miuiNotificationHeaderView.methodFinder()
            .filterByName("onFinishInflate")
            .single().createAfterHook { param ->
                val viewGroup = param.thisObject as ViewGroup
                val context = viewGroup.context

                val dateView = viewGroup.findViewById<View>(
                    context.resources.getIdentifier(
                        "date_time",
                        "id",
                        context.packageName
                    )
                )
                val landClock = viewGroup.findViewById<View>(
                    context.resources.getIdentifier(
                        "horizontal_time",
                        "id",
                        context.packageName
                    )
                )

                addWeatherViewAfterOf(dateView, ORIENTATION_PORTRAIT)
                addWeatherViewAfterOf(landClock, ORIENTATION_LANDSCAPE)
            }
    }

    private fun addWeatherViewAfterOf(view: View, @Orientation key: Int) {
        val weatherView = WeatherView(view.context, isDisplayCity).apply {
            var appearance = "TextAppearance."
            when (key) {
                ORIENTATION_PORTRAIT -> {
                    vWeatherView = this
                    appearance += "QSControl.Date"
                }

                ORIENTATION_LANDSCAPE -> {
                    hWeatherView = this
                    appearance += "NSNotification.Clock"
                }
            }

            val resources = context.resources
            setTextAppearance(resources.getIdentifier(appearance, "style", context.packageName))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = resources.getDimensionPixelSize(
                    resources.getIdentifier(
                        "notification_panel_time_date_space",
                        "dimen",
                        context.packageName
                    )
                ) + dp2px(5f)
            }

            setOnClickListener {
                startWeatherApp()
            }
        }

        val viewParent = view.parent as ViewGroup
        viewParent.addView(weatherView, viewParent.indexOfChild(view) + 1)
    }

    @IntDef(value = [ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Orientation
}
