package com.sevtinge.cemiuiler.module.systemui.statusbar.icon.all

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.devicesdk.isAndroidR
import com.sevtinge.cemiuiler.utils.devicesdk.isMoreAndroidVersion
import de.robv.android.xposed.XposedHelpers

object BatteryStyle : BaseHook() {
    override fun init() {
        val mBatteryMeterViewClass = when {
            isAndroidR() -> loadClass("com.android.systemui.MiuiBatteryMeterView")
            isMoreAndroidVersion(31) -> loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView")
            else -> null
        }

        if (isAndroidR()) {
            mBatteryMeterViewClass!!.methodFinder().first {
                name == "updateResources"
            }
        } else {
            mBatteryMeterViewClass!!.methodFinder().first {
                name == "updateAll"
            }
        }.createHook {
            after { param ->
                val batteryView = param.thisObject as LinearLayout
                val res = batteryView.resources
                val mBatteryTextDigitView =
                    XposedHelpers.getObjectField(param.thisObject, "mBatteryTextDigitView") as TextView
                val mBatteryPercentView =
                    XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView") as TextView
                val mBatteryPercentMarkView =
                    XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView") as TextView

                if (mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location")) {
                    batteryView.removeView(mBatteryPercentView)
                    batteryView.removeView(mBatteryPercentMarkView)
                    batteryView.addView(mBatteryPercentMarkView, 0)
                    batteryView.addView(mBatteryPercentView, 0)
                }

                if (mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom")) {
                    val fontSize =
                        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_size", 15) * 0.5f
                    if (fontSize > 7.5) {
                        mBatteryTextDigitView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                        mBatteryPercentView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                    }
                    val fontSizeMark =
                        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_mark_size", 15) * 0.5f
                    if (fontSizeMark > 7.5) {
                        mBatteryPercentMarkView.setTextSize(
                            TypedValue.COMPLEX_UNIT_DIP,
                            fontSizeMark
                        )
                    }

                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_style_bold")) {
                        mBatteryTextDigitView.typeface = Typeface.DEFAULT_BOLD
                        mBatteryPercentView.typeface = Typeface.DEFAULT_BOLD
                    }

                    var leftMargin: Int =
                        mPrefsMap.getInt("system_ui_status_bar_battery_style_left_margin", 0)
                    var rightMargin: Int =
                        mPrefsMap.getInt("system_ui_status_bar_battery_style_right_margin", 0)

                    leftMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, leftMargin * 0.5f, res.displayMetrics
                    ).toInt()
                    rightMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, rightMargin * 0.5f, res.displayMetrics
                    ).toInt()

                    var topMargin = 0
                    val verticalOffset: Int =
                        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset", 8)
                    if (verticalOffset != 8) {
                        val marginTop =
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, (verticalOffset - 8) * 0.5f, res.displayMetrics
                            )
                        topMargin = marginTop.toInt()
                    }

                    var digitRightMargin = 0
                    var markRightMargin = 0
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")) {
                        digitRightMargin = rightMargin
                    } else {
                        markRightMargin = rightMargin
                    }
                    if (leftMargin > 0 || topMargin != 8 || digitRightMargin > 0) {
                        mBatteryPercentView.setPaddingRelative(
                            leftMargin, topMargin, digitRightMargin, 0
                        )
                    }

                    val verticalOffsetMark =
                        mPrefsMap.getInt(
                            "system_ui_status_bar_battery_style_vertical_offset_mark",
                            17
                        )
                    if (verticalOffsetMark < 17) {
                        val marginTop =
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, (verticalOffsetMark - 8) * 0.5f, res.displayMetrics
                            )
                        topMargin = marginTop.toInt()
                    }
                    if (verticalOffset < 17 || markRightMargin > 0) {
                        mBatteryPercentMarkView.setPaddingRelative(0, topMargin, markRightMargin, 0)
                    }
                }
            }
        }
    }
}
