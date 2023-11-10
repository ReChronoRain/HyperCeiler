package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

@SuppressLint("StaticFieldLeak")
object BatteryStyle : BaseHook() {
    private val verticalOffsetMark by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset_mark", 27)
    }

    private val mBatteryMeterViewClass = loadClassOrNull("com.android.systemui.statusbar.views.MiuiBatteryMeterView")

    private lateinit var batteryView: LinearLayout
    private lateinit var mBatteryPercentView: TextView
    private lateinit var mBatteryPercentMarkView: TextView

    override fun init() {
       mBatteryMeterViewClass!!.methodFinder().first {
           name == "updateAll"
       }.createHook {
           after { param ->
               batteryView = param.thisObject as LinearLayout
               mBatteryPercentView =
                   XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView") as TextView
               mBatteryPercentMarkView =
                   XposedHelpers.getObjectField(
                       param.thisObject,
                       "mBatteryPercentMarkView"
                   ) as TextView

               // 交换电池图标与电量位置（电量外显下才能正常交换）
               if (mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location")) {
                   batteryView.removeView(mBatteryPercentView)
                   batteryView.removeView(mBatteryPercentMarkView)
                   batteryView.addView(mBatteryPercentMarkView, 0)
                   batteryView.addView(mBatteryPercentView, 0)
               }
               // 自定义部分
               enableCustom(param)
           }
       }
    }

    private fun enableCustom(param: XC_MethodHook.MethodHookParam) {
        val res = batteryView.resources
        val mBatteryTextDigitView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryTextDigitView") as TextView

        if (!mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom")) return

        try {
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
            if (verticalOffset != 12) {
                val marginTop =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        (verticalOffset - 12) * 0.5f,
                        res.displayMetrics
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

            if (verticalOffsetMark < 27) {
                val marginTop =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        (verticalOffsetMark - 8) * 0.5f,
                        res.displayMetrics
                    )
                topMargin = marginTop.toInt()
            }
            if (verticalOffsetMark < 27 || markRightMargin > 0) {
                mBatteryPercentMarkView.setPaddingRelative(0, topMargin, markRightMargin, 0)
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
        }
    }
}
