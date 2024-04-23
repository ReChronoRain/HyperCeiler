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
import androidx.constraintlayout.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.view.*


object NotificationWeather : BaseHook() {
    @SuppressLint("DiscouragedApi", "ServiceCast")
    override fun init() {
        var mWeatherView: TextView? = null
        var mConstraintLayout: ConstraintLayout? = null
        val isDisplayCity = mPrefsMap.getBoolean("system_ui_control_center_show_weather_city")
        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("onFinishInflate")
            .single().createHook {
                after { param ->
                    val viewGroup = param.thisObject as ViewGroup
                    val context = viewGroup.context

                    // MIUI编译时间大于 2022-03-12 00:00:00 且为内测版
                    if ((PropUtils.getProp(context, "ro.build.date.utc").toInt() >= 1647014400 &&
                            !PropUtils.getProp(context, "ro.build.version.incremental")
                                .endsWith("XM")) &&
                        !isMoreHyperOSVersion(1f)
                    ) {
                        // 获取原组件
                        val bigTimeId =
                            context.resources.getIdentifier("big_time", "id", context.packageName)
                        val bigTime: TextView = viewGroup.findViewById(bigTimeId)

                        val dateTimeId =
                            context.resources.getIdentifier("date_time", "id", context.packageName)
                        val dateTime: TextView = viewGroup.findViewById(dateTimeId)

                        // 创建新布局
                        val mConstraintLayoutLp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.topMargin = context.resources.getDimensionPixelSize(
                                context.resources.getIdentifier(
                                    "qs_control_header_tiles_margin_top",
                                    "dimen",
                                    context.packageName
                                )
                            )
                        }

                        mConstraintLayout =
                            ConstraintLayout(context).also { it.layoutParams = mConstraintLayoutLp }

                        (bigTime.parent as ViewGroup).addView(mConstraintLayout, 0)


                        // 从原布局中删除组件
                        (bigTime.parent as ViewGroup).removeView(bigTime)
                        (dateTime.parent as ViewGroup).removeView(dateTime)


                        // 添加组件至新布局
                        mConstraintLayout!!.addView(bigTime)
                        mConstraintLayout!!.addView(dateTime)

                        // 组件属性
                        val dateTimeLp = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.startToEnd = bigTimeId
                            it.bottomToBottom = 0
                            it.marginStart = context.resources.getDimensionPixelSize(
                                context.resources.getIdentifier(
                                    "notification_panel_time_date_space",
                                    "dimen",
                                    context.packageName
                                )
                            )
                            it.bottomMargin = dp2px(5f)
                        }
                        dateTime.layoutParams = dateTimeLp


                        // 创建天气组件
                        mWeatherView = WeatherView(context, isDisplayCity).apply {
                            setTextAppearance(
                                context.resources.getIdentifier(
                                    "TextAppearance.QSControl.Date",
                                    "style",
                                    context.packageName
                                )
                            )

                        }
                        mConstraintLayout!!.addView(mWeatherView)

                        val mweatherviewLp = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.startToEnd = bigTimeId
                            it.bottomToTop = dateTimeId
                            it.marginStart = context.resources.getDimensionPixelSize(
                                context.resources.getIdentifier(
                                    "notification_panel_time_date_space",
                                    "dimen",
                                    context.packageName
                                )
                            )
                        }

                        (mWeatherView as WeatherView).layoutParams = mweatherviewLp

                    } else {

                        val layoutParam =
                            loadClass("androidx.constraintlayout.widget.ConstraintLayout\$LayoutParams").getConstructor(
                                Int::class.java,
                                Int::class.java
                            ).newInstance(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ) as ViewGroup.MarginLayoutParams


                        layoutParam.setObjectField(
                            "bottomToTop",
                            context.resources.getIdentifier("date_time", "id", context.packageName)
                        )
                        layoutParam.setObjectField(
                            "startToEnd",
                            context.resources.getIdentifier("big_time", "id", context.packageName)
                        )


                        layoutParam.marginStart = context.resources.getDimensionPixelSize(
                            context.resources.getIdentifier(
                                "notification_panel_time_date_space",
                                "dimen",
                                context.packageName
                            )
                        )
                        mWeatherView = WeatherView(context, isDisplayCity).apply {
                            setTextAppearance(
                                context.resources.getIdentifier(
                                    "TextAppearance.QSControl.Date",
                                    "style",
                                    context.packageName
                                )
                            )
                            layoutParams = layoutParam
                        }
                        viewGroup.addView(mWeatherView)
                    }

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
                            Toast.makeText(context, "启动失败", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("updateLayout")
            .single().createHook {
                after {
                    val viewGroup = it.thisObject as ViewGroup
                    val context = viewGroup.context
                    val mOrientation = viewGroup.getObjectField("mOrientation") as Int
                    // MIUI编译时间大于 2022-03-12 00:00:00 且为内测版
                    if (PropUtils.getProp(context, "ro.build.date.utc").toInt() >= 1647014400 &&
                        !PropUtils.getProp(context, "ro.build.version.incremental").endsWith(
                            "DEV"
                        ) &&
                        !PropUtils.getProp(context, "ro.build.version.incremental").endsWith("XM")
                    ) {
                        if (mOrientation == 1) {
                            mConstraintLayout!!.visibility = View.VISIBLE
                        } else {
                            mConstraintLayout!!.visibility = View.GONE
                        }
                    } else {
                        if (mOrientation == 1) {
                            mWeatherView!!.visibility = View.VISIBLE
                        } else {
                            mWeatherView!!.visibility = View.GONE
                        }
                    }
                }
            }
    }
}
