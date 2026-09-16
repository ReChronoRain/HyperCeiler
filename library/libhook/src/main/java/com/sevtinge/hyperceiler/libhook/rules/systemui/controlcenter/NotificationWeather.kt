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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IntDef
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.miuiConfigs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.WeatherView
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getDimenByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethodAs
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs

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
        PrefsBridge.getBoolean("system_ui_control_center_show_weather_city")
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
        newNotificationWeather()

        // 更新资源
        updateResources()
        // 更新布局
        updateLayout()
    }

    private fun updateResources() {
        val method = miuiNotificationHeaderView.findMethodOrNull {
            name("updateHeaderResources")
        } ?: miuiNotificationHeaderView.findMethod {
            name("updateResources")
        }

        method.createAfterHook { param ->
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
        miuiNotificationHeaderView.findMethod { name("updateLayout") }
            .createBeforeHook {
                val viewGroup = it.thisObject as ViewGroup
                val context = viewGroup.context
                val configuration = context.resources.configuration
                val orientation = viewGroup.getObjectFieldAs<Int>("mOrientation")
                val screenLayout = viewGroup.getObjectFieldAs<Int>("mScreenLayout")

                if (orientation == configuration.orientation &&
                    screenLayout == configuration.screenLayout
                ) {
                    return@createBeforeHook
                }

                val isVerticalMode =
                    miuiConfigs.callStaticMethodAs<Boolean>("isVerticalMode", context)

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

            if (!isHyperOSVersion(4f)) {
                addWeatherViewAfterOf(dateView, ORIENTATION_PORTRAIT)
                addWeatherViewAfterOf(landClock, ORIENTATION_LANDSCAPE)
            } else {
                addWeatherViewAfterOfOS4(dateView, ORIENTATION_PORTRAIT)
                addWeatherViewAfterOfOS4(landClock, ORIENTATION_LANDSCAPE)
            }

            // 创建动画
            hWeatherView?.let {
                hWeatherViewFolme = folme.callStaticMethod("useAt", arrayOf<View>(it))
            }
            vWeatherView?.let {
                vWeatherViewFolme = folme.callStaticMethod("useAt", arrayOf<View>(it))
            }
        }

        combinedHeaderController.findMethod { name("onSwitchProgressChanged"); parameterTypes(Float::class.java) }
            .createAfterHook { param ->
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
        clazz.findMethod { name("onAppearanceChanged") }.createAfterHook {
            val newAppearance = it.args[0] as Boolean
            val animate = it.args[1] as Boolean

            val startFolmeAnimationAlpha = { view: View?, folme: Any? ->
                notificationHeaderExpandController.callStaticMethod(
                    $$"access$startFolmeAnimationAlpha",
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

        clazz.findMethod { name("onExpansionChanged") }.createAfterHook {
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

    private fun addWeatherViewAfterOfOS4(
        view: View,
        @Orientation key: Int
    ) {
        val parent = view.parent as? ViewGroup ?: return
        val context = view.context

        Log.d(
            "NotificationWeather",
            "Adding weather view after " +
                "${view.javaClass.simpleName} in " +
                "${parent.javaClass.simpleName}, " +
                "orientation=$key"
        )

        val weatherView = WeatherView(
            context,
            isDisplayCity
        ).apply {
            id = View.generateViewId()

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

            setTextAppearance(
                context.getIdByName(
                    appearance,
                    "style"
                )
            )

            /*
             * 这里最好不要使用你模块自己的 ConstraintLayout.LayoutParams。
             *
             * 因为 parent 是 SystemUI ClassLoader 的
             * ConstraintLayout。
             *
             * 虽然 LayoutParams 本身很多情况下可以工作，
             * 但为了彻底避免 ClassLoader 类型问题，
             * 后面可以进一步改成从 parent.generateLayoutParams() 获取。
             */
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setOnClickListener {
                startWeatherApp()
            }
        }

        val index = parent.indexOfChild(view)

        if (index < 0) {
            XposedLog.e(
                TAG,
                lpparam.packageName,
                "Target view is no longer child of parent"
            )
            return
        }

        parent.addView(
            weatherView,
            index + 1
        )

        XposedLog.d(
            TAG,
            lpparam.packageName,
            "Weather added: " +
                "key=$key, " +
                "weatherId=${weatherView.id}, " +
                "targetId=${view.id}, " +
                "parent=${parent.javaClass.name}, " +
                "childCount=${parent.childCount}"
        )

        /*
         * 不再 weatherView.post {}
         *
         * 等 parent 完成 layout。
         */
        waitForConstraintLayout(
            weatherView = weatherView,
            targetView = view,
            parent = parent,
            key = key
        )
    }

    private fun waitForConstraintLayout(
        weatherView: View,
        targetView: View,
        parent: ViewGroup,
        @Orientation key: Int
    ) {
        /*
         * View 已经被移除/替换。
         */
        if (weatherView.parent !== parent) {
            Log.d(
                "NotificationWeather",
                "Weather parent changed, abort: " +
                    "weatherParent=${weatherView.parent?.javaClass?.name}"
            )
            return
        }

        /*
         * target 已经不是当前 parent 的 child。
         *
         * 横竖屏切换的时候非常重要。
         */
        if (targetView.parent !== parent) {
            Log.d(
                "NotificationWeather",
                "Target is no longer in parent, abort: " +
                    "targetParent=${targetView.parent?.javaClass?.name}"
            )
            return
        }

        /*
         * parent 和 target 都完成 layout。
         */
        if (
            parent.isLaidOut &&
            targetView.isLaidOut &&
            parent.width > 0 &&
            parent.height > 0 &&
            targetView.width > 0 &&
            targetView.height > 0
        ) {
            XposedLog.d(
                TAG,
                lpparam.packageName,
                "Layout ready: " +
                    "key=$key, " +
                    "parent=${parent.width}x${parent.height}, " +
                    "target=${targetView.width}x${targetView.height}"
            )

            constrainWeatherView(
                weatherView,
                targetView
            )

            return
        }

        XposedLog.d(
            TAG,
            lpparam.packageName,
            "Layout not ready, waiting: " +
                "key=$key, " +
                "parentLaidOut=${parent.isLaidOut}, " +
                "targetLaidOut=${targetView.isLaidOut}, " +
                "parent=${parent.width}x${parent.height}, " +
                "target=${targetView.width}x${targetView.height}"
        )

        val listener = object : View.OnLayoutChangeListener {

            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                /*
                 * target / weather 已经脱离当前 parent。
                 */
                if (
                    weatherView.parent !== parent ||
                    targetView.parent !== parent
                ) {
                    parent.removeOnLayoutChangeListener(this)

                    XposedLog.d(
                        TAG,
                        lpparam.packageName,
                        "Layout listener removed: hierarchy changed"
                    )

                    return
                }

                /*
                 * parent layout 完成后，
                 * target 也应该已经拥有有效尺寸。
                 */
                if (
                    parent.isLaidOut &&
                    targetView.isLaidOut &&
                    parent.width > 0 &&
                    parent.height > 0 &&
                    targetView.width > 0 &&
                    targetView.height > 0
                ) {
                    parent.removeOnLayoutChangeListener(this)

                    XposedLog.d(
                        TAG,
                        lpparam.packageName,
                        "Layout became ready: " +
                            "key=$key, " +
                            "parent=${parent.width}x${parent.height}, " +
                            "target=${targetView.width}x${targetView.height}"
                    )

                    constrainWeatherView(
                        weatherView,
                        targetView
                    )
                }
            }
        }

        parent.addOnLayoutChangeListener(listener)

        /*
         * 强制触发一次 layout。
         */
        parent.requestLayout()
    }

    private fun constrainWeatherView(
        weatherView: View,
        targetView: View
    ) {
        XposedLog.d(
            TAG,
            lpparam.packageName,
            "constrainWeatherView ENTER: " +
                "weatherId=${weatherView.id}, " +
                "targetId=${targetView.id}"
        )

        val parent = weatherView.parent

        if (parent == null) {
            XposedLog.e(
                TAG,
                lpparam.packageName,
                "Constraint parent FAILED: parent=null"
            )
            return
        }

        val classLoader = parent.javaClass.classLoader

        XposedLog.d(
            TAG,
            lpparam.packageName,
            "Parent: ${parent.javaClass.name}"
        )

        XposedLog.d(
            TAG,
            lpparam.packageName,
            "SystemUI ClassLoader: $classLoader"
        )

        if (weatherView.id == View.NO_ID ||
            targetView.id == View.NO_ID
        ) {
            XposedLog.d(
                TAG,
                lpparam.packageName,
                "Invalid IDs: weather=${weatherView.id}, " +
                    "target=${targetView.id}"
            )
            return
        }

        try {
            /*
             * 必须使用 SystemUI 自己的 ClassLoader。
             */
            val constraintLayoutClass = Class.forName(
                "androidx.constraintlayout.widget.ConstraintLayout",
                false,
                classLoader
            )

            if (!constraintLayoutClass.isInstance(parent)) {
                XposedLog.e(
                    TAG,
                    lpparam.packageName,
                    "Parent is not SystemUI ConstraintLayout: " +
                        "parent=${parent.javaClass.name}"
                )
                return
            }

            XposedLog.d(
                TAG,
                lpparam.packageName,
                "SystemUI ConstraintLayout verified"
            )

            /*
             * 获取 SystemUI 自己的 ConstraintSet。
             */
            val constraintSetClass = Class.forName(
                "androidx.constraintlayout.widget.ConstraintSet",
                false,
                classLoader
            )

            val constraintSet =
                constraintSetClass.getDeclaredConstructor().apply {
                    isAccessible = true
                }.newInstance()

            XposedLog.d(
                TAG,
                lpparam.packageName,
                "Before clone: weather=${weatherView.id}"
            )

            constraintSetClass
                .getMethod(
                    "clone",
                    constraintLayoutClass
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    parent
                )

            XposedLog.d(
                TAG,
                lpparam.packageName,
                "After clone: weather=${weatherView.id}"
            )

            /*
             * ConstraintSet 常量
             */
            val START = 6
            val END = 7
            val TOP = 3
            val BOTTOM = 4

            val WRAP_CONTENT = -2

            /*
             * clear(int)
             */
            constraintSetClass
                .getMethod(
                    "clear",
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id
                )

            /*
             * constrainWidth(int, int)
             */
            constraintSetClass
                .getMethod(
                    "constrainWidth",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id,
                    WRAP_CONTENT
                )

            /*
             * constrainHeight(int, int)
             */
            constraintSetClass
                .getMethod(
                    "constrainHeight",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id,
                    WRAP_CONTENT
                )

            /*
             * START(weather) -> END(target)
             */
            constraintSetClass
                .getMethod(
                    "connect",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id,
                    START,
                    targetView.id,
                    END,
                    dp2px(5f)
                )

            /*
             * TOP(weather) -> TOP(target)
             */
            constraintSetClass
                .getMethod(
                    "connect",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id,
                    TOP,
                    targetView.id,
                    TOP
                )

            /*
             * BOTTOM(weather) -> BOTTOM(target)
             */
            constraintSetClass
                .getMethod(
                    "connect",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    weatherView.id,
                    BOTTOM,
                    targetView.id,
                    BOTTOM
                )

            XposedLog.d(
                TAG,
                lpparam.packageName,
                "Before applyTo: " +
                    "weather=${weatherView.id} -> target=${targetView.id}"
            )

            /*
             * applyTo(ConstraintLayout)
             */
            constraintSetClass
                .getMethod(
                    "applyTo",
                    constraintLayoutClass
                )
                .apply {
                    isAccessible = true
                }
                .invoke(
                    constraintSet,
                    parent
                )

            XposedLog.d(
                TAG,
                lpparam.packageName,
                "Constraints APPLIED successfully: " +
                    "weather=${weatherView.id} -> target=${targetView.id}"
            )

        } catch (e: Throwable) {
            XposedLog.e(
                TAG,
                lpparam.packageName,
                "Failed to apply weather constraints",
                e
            )
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
            setTextAppearance(
                context.getIdByName(appearance, "style")
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = resources.getDimensionPixelSize(
                    context.getDimenByName("notification_panel_time_date_space")
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
