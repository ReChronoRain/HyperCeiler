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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

object HideBatteryIcon : BaseHook() {
    override fun init() {
        val mBatteryMeterViewClass by lazy {
            loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView")
        }

        if (isMoreAndroidVersion(35)) {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateAll\$1")
        } else if (isAndroidVersion(34)) {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateAll")
        } else {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateResources")
        }.single().createHook {
            after { param ->
                if (param.thisObject != null) {
                    // 隐藏电池图标
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_icon")) {
                        (param.thisObject.getObjectFieldAs<ImageView>("mBatteryIconView")).visibility =
                            View.GONE

                        if (param.thisObject.getObjectField("mBatteryStyle") == 1) {
                            (param.thisObject.getObjectFieldAs<FrameLayout>("mBatteryDigitalView")).visibility =
                                View.GONE
                        }
                    }
                    // 隐藏电池百分号
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") ||
                        mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")
                    ) {
                        (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentMarkView"))?.textSize = 0F
                    }
                    // 隐藏电池内的百分比
                    if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                        (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentView"))?.textSize = 0F
                        (param.thisObject?.getObjectFieldAs<TextView>("mBatteryTextDigitView"))?.textSize = 0F
                    }
                }
            }
        }

        mBatteryMeterViewClass.methodFinder()
            .filterByName("updateChargeAndText")
            .single().createHook {
                after { param ->
                    // 隐藏电池充电图标
                    if (param.thisObject != null) {
                        if (isMoreAndroidVersion(35)) {
                            // 隐藏电池百分号
                            if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") ||
                                mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")
                            ) {
                                (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentMarkView"))?.textSize = 0F
                            }
                            // 隐藏电池内的百分比
                            if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                                (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentView"))?.textSize = 0F
                                (param.thisObject?.getObjectFieldAs<TextView>("mBatteryTextDigitView"))?.textSize = 0F
                            }
                        }

                        if (mPrefsMap.getBoolean("system_ui_status_bar_battery_charging")) {
                            (param.thisObject.getObjectFieldAs<ImageView>("mBatteryChargingInView")).visibility =
                                View.GONE
                            (param.thisObject.getObjectFieldAs<ImageView>("mBatteryChargingView")).visibility =
                                View.GONE
                        }
                    }
                }
            }
    }

}
