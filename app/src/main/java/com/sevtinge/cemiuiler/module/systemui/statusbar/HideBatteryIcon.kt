package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook

object HideBatteryIcon : BaseHook() {
    override fun init() {
        findMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView") {
            name == "updateResources"
        }.hookAfter {
            //隐藏电池图标
            if (mPrefsMap.getBoolean("system_ui_status_bar_battery_icon")) {
                (it.thisObject.getObjectAs<ImageView>("mBatteryIconView")).visibility = View.GONE
                if (it.thisObject.getObject("mBatteryStyle") == 1) {
                    (it.thisObject.getObjectAs<FrameLayout>("mBatteryDigitalView")).visibility =
                        View.GONE
                }
            }

            if (!mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") || mPrefsMap.getBoolean(
                    "system_ui_status_bar_battery_percent_mark"
                )
            ) { //隐藏电池百分号
                (it.thisObject.getObjectAs<TextView>("mBatteryPercentMarkView")).textSize = 0F
            } else if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) { //隐藏电池内的百分比
                (it.thisObject.getObjectAs<TextView>("mBatteryPercentView")).textSize = 0F
                (it.thisObject.getObjectAs<TextView>("mBatteryTextDigitView")).textSize = 0F
                (it.thisObject.getObjectAs<TextView>("mBatteryPercentMarkView")).textSize = 0F
            }
        }

        findMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView") {
            name == "updateChargeAndText"
        }.hookAfter {
            //隐藏电池充电图标
            if (mPrefsMap.getBoolean("system_ui_status_bar_battery_charging")) {
                (it.thisObject.getObjectAs<ImageView>("mBatteryChargingInView")).visibility =
                    View.GONE
                (it.thisObject.getObjectAs<ImageView>("mBatteryChargingView")).visibility =
                    View.GONE
            }
        }
    }

}