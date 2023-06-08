package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField
import com.sevtinge.cemiuiler.utils.getObjectFieldAs

object HideBatteryIcon : BaseHook() {
    override fun init() {
        findAndHookMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView",
            "updateResources",
            object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    // 隐藏电池图标
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_icon")) {
                        (param?.thisObject?.getObjectFieldAs<ImageView>("mBatteryIconView"))?.visibility =
                            View.GONE
                        if (param?.thisObject?.getObjectField("mBatteryStyle") == 1) {
                            (param.thisObject?.getObjectFieldAs<FrameLayout>("mBatteryDigitalView"))?.visibility =
                                View.GONE
                        }
                    }
                    // 隐藏电池百分号
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") || mPrefsMap.getBoolean(
                            "system_ui_status_bar_battery_percent_mark"
                        )
                    ) {
                        (param?.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentMarkView"))?.textSize =
                            0F
                    }
                    // 隐藏电池内的百分比
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                        (param?.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentView"))?.textSize =
                            0F
                        (param?.thisObject?.getObjectFieldAs<TextView>("mBatteryTextDigitView"))?.textSize =
                            0F
                    }
                }
            }
        )

        findAndHookMethod("com.android.systemui.statusbar.views.MiuiBatteryMeterView",
            "updateChargeAndText",
            object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    // 隐藏电池充电图标
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_charging")) {
                        (param?.thisObject?.getObjectFieldAs<ImageView>("mBatteryChargingInView"))?.visibility =
                            View.GONE
                        (param?.thisObject?.getObjectFieldAs<ImageView>("mBatteryChargingView"))?.visibility =
                            View.GONE
                    }
                }
            })
    }

}
