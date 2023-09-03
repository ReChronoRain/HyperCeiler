package com.sevtinge.cemiuiler.module.hook.securitycenter

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DisplayUtils.dip2px
import com.sevtinge.cemiuiler.utils.findClassOrNull
import com.sevtinge.cemiuiler.utils.getObjectFieldAs
import com.sevtinge.cemiuiler.utils.isStatic

object ShowBatteryTemperatureNew : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {

        // if (!getBoolean("securitycenter_show_battery_temperature", false)) return
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
            loadClass("com.miui.powercenter.BatteryFragment\$a").methodFinder().first {
                name == "run"
            }
        } else {
            loadClass("com.miui.powercenter.a\$a").methodFinder().first {
                name == "run"
            }
        }.createHook {
            after { hookParam ->
                val context = AndroidAppHelper.currentApplication().applicationContext
                val isDarkMode =
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val currentTemperatureState =
                    context.resources.getIdentifier("current_temperature_state", "id", "com.miui.securitycenter")
                val view = hookParam.thisObject.getObjectFieldAs<View>("a")

                val textView = view.findViewById<TextView>(currentTemperatureState)
                textView.apply {
                    when (layoutParams) {
                        is LinearLayout.LayoutParams -> {
                            (layoutParams as LinearLayout.LayoutParams).topMargin = 0
                            setPadding(0, dip2px(context, 4f), 0, 0)
                            height = dip2px(context, 49f)
                        }
                    }
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36.4f)
                    gravity = Gravity.NO_GRAVITY
                    typeface = Typeface.create(null, 700, false)
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }

                val temperatureContainer =
                    context.resources.getIdentifier("temperature_container", "id", "com.miui.securitycenter")
                when (val childView = view.findViewById<LinearLayout>(temperatureContainer).getChildAt(1)) {
                    is LinearLayout -> {
                        childView.orientation = LinearLayout.VERTICAL
                        val l1 = childView.getChildAt(0)
                        val l2 = childView.getChildAt(1)
                        val linearLayout = LinearLayout(context)
                        val linearLayout1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                        val tempView = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            (layoutParams as LinearLayout.LayoutParams).marginStart = dip2px(context, 3.6f)
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.1f)
                            setTextColor(Color.parseColor(if (isDarkMode) "#e6e6e6" else "#333333"))
                            setPadding(0, dip2px(context, 26f), 0, 0)
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
                            (layoutParams as RelativeLayout.LayoutParams).topMargin = -dip2px(context, 0.78f)
                        }
                        val tempView = TextView(context).apply {
                            layoutParams = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                            ).also {
                                it.addRule(RelativeLayout.END_OF, l2.id)
                                it.addRule(RelativeLayout.ALIGN_BOTTOM, l2.id)
                            }
                            setPadding(dip2px(context, 3.6f), 0, 0, dip2px(context, 5.9f))
                            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.1f)
                            setTextColor(Color.parseColor(if (isDarkMode) "#e6e6e6" else "#333333"))
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
        )!!
            .getIntExtra("temperature", 0) / 10
    }
}
