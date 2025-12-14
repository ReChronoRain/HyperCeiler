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
package com.sevtinge.hyperceiler.hook.module.rules.securitycenter.battery

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.findClassOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.isStatic
import io.github.kyuubiran.ezxhelper.core.extension.MemberExtension.paramCount
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object ShowBatteryTemperatureNew : BaseHook() {
    private val smartChargeClazz by lazy<Method> {
        DexKit.findMember("SmartChargeClazz") {
            it.findMethod {
                searchPackages("com.miui.powercenter.nightcharge")
                matcher {
                    paramCount = 1
                    modifiers = Modifier.STATIC

                    addInvoke("Ljava/lang/Math;->abs(I)I")
                }
            }.single()
        }
    }

    override fun init() {
        try {
            newBatteryTemperature()
        } catch (_: Throwable) {
            oldBatteryTemperature()
        }
    }

    private fun newBatteryTemperature() {
        smartChargeClazz.createHook {
            after {
                it.result = getBatteryTemperature(it.args[0] as Context).toString() + " ℃"
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun oldBatteryTemperature() {
        val batteryFragmentClass = "com.miui.powercenter.BatteryFragment".findClassOrNull()
        if (batteryFragmentClass != null) {
            loadClass("com.miui.powercenter.BatteryFragment").methodFinder().first {
                paramCount == 1 && returnType == String::class.java && isStatic
            }
        } else {
            loadClass("com.miui.powercenter.a").methodFinder().first {
                paramCount == 1 && returnType == String::class.java && isStatic
            }
        }.createHook {
            after {
                it.result = getBatteryTemperature(it.args[0] as Context).toString()
            }
        }

        if (batteryFragmentClass != null) {
            loadClass($$"com.miui.powercenter.BatteryFragment$a").methodFinder().first {
                name == "run"
            }
        } else {
            loadClass($$"com.miui.powercenter.a$a").methodFinder().first {
                name == "run"
            }
        }.createHook {
            after { hookParam ->
                val context = AndroidAppHelper.currentApplication().applicationContext
                val isDarkMode =
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val currentTemperatureState =
                    context.resources.getIdentifier(
                        "current_temperature_state",
                        "id",
                        "com.miui.securitycenter"
                    )
                val view = hookParam.thisObject.getObjectFieldAs<View>("a")

                val textView = view.findViewById<TextView>(currentTemperatureState)
                textView.apply {
                    when (layoutParams) {
                        is LinearLayout.LayoutParams -> {
                            (layoutParams as LinearLayout.LayoutParams).topMargin = 0
                            setPadding(0, dp2px(4f), 0, 0)
                            height = dp2px(49f)
                        }
                    }
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36.4f)
                    gravity = Gravity.NO_GRAVITY
                    typeface = Typeface.create(null, 700, false)
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }

                val temperatureContainer =
                    context.resources.getIdentifier(
                        "temperature_container",
                        "id",
                        "com.miui.securitycenter"
                    )
                when (val childView =
                    view.findViewById<LinearLayout>(temperatureContainer).getChildAt(1)) {
                    is LinearLayout -> {
                        childView.orientation = LinearLayout.VERTICAL
                        val l1 = childView.getChildAt(0)
                        val l2 = childView.getChildAt(1)
                        val linearLayout = LinearLayout(context)
                        val linearLayout1 =
                            LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                        val tempView = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            (layoutParams as LinearLayout.LayoutParams).marginStart =
                                dp2px(3.6f)
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.1f)
                            setTextColor(if (isDarkMode) "#e6e6e6".toColorInt() else "#333333".toColorInt())
                            setPadding(0, dp2px(26f), 0, 0)
                            text = "℃"
                            gravity = Gravity.NO_GRAVITY
                            typeface = Typeface.create(null, 700, false)
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        }
                        childView.removeAllViews()
                        linearLayout.addView(l1)
                        linearLayout1.addView(l2)
                        linearLayout1.addView(tempView)
                        childView.addView(linearLayout)
                        childView.addView(linearLayout1)
                    }

                    is RelativeLayout -> {
                        val relativeLayout = RelativeLayout(context)
                        val l1 = childView.getChildAt(0)
                        val l2 = childView.getChildAt(1).apply {
                            layoutParams = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                            ).also {
                                it.addRule(RelativeLayout.BELOW, l1.id)
                                it.addRule(RelativeLayout.ALIGN_START, l1.id)
                            }
                            (layoutParams as RelativeLayout.LayoutParams).topMargin =
                                -dp2px(0.78f)
                        }
                        val tempView = TextView(context).apply {
                            layoutParams = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                            ).also {
                                it.addRule(RelativeLayout.END_OF, l2.id)
                                it.addRule(RelativeLayout.ALIGN_BOTTOM, l2.id)
                            }
                            setPadding(dp2px(3.6f), 0, 0, dp2px(5.9f))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.1f)
                            setTextColor(if (isDarkMode) "#e6e6e6".toColorInt() else "#333333".toColorInt())
                            text = "℃"
                            gravity = Gravity.NO_GRAVITY
                            typeface = Typeface.create(null, 700, false)
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        }
                        childView.removeAllViews()
                        relativeLayout.addView(l1)
                        relativeLayout.addView(l2)
                        relativeLayout.addView(tempView)
                        childView.addView(relativeLayout)
                    }
                }
            }
        }
    }

    private fun getBatteryTemperature(context: Context): Int {
        return context.registerReceiver(
            null as BroadcastReceiver?,
            IntentFilter("android.intent.action.BATTERY_CHANGED")
        )!!.getIntExtra("temperature", 0) / 10
    }
}
