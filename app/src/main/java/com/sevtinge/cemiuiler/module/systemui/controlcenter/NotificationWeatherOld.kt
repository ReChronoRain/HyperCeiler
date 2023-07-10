package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.setObjectField
import com.sevtinge.cemiuiler.view.WeatherView

object NotificationWeatherOld : BaseHook() {

    @SuppressLint("DiscouragedApi")
    override fun init() {
        var mWeatherView: TextView?
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        loadClass("com.android.systemui.qs.MiuiQSHeaderView").methodFinder().first {
            name == "onFinishInflate"
        }.createHook {
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
                }


                viewGroup.addView(mWeatherView)
                (mWeatherView as WeatherView).setOnClickListener {
                    try {
                        val intent = Intent().apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            component = ComponentName(
                                "com.miui.weather2",
                                "com.miui.weather2.ActivityWeatherMain"
                            )
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        logE(e)
                    }
                }
            }
        }
    }

}
