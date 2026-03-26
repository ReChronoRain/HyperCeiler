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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.b

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object HideBatteryIconB : BaseHook() {
    override fun init() {
        val mBatteryMeterViewClass by lazy {
            loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView")
        }

        mBatteryMeterViewClass.methodFinder()
            .filterByName("onBatteryStyleChanged")
            .first().createHook {
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
                    }
                }
            }

        mBatteryMeterViewClass.methodFinder()
            .filterByName("updateAll")
            .single().createHook {
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
                        // 隐藏电池的百分比
                        // Todo：内显百分比暂无法隐藏，因为布局已修改
                        if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                            (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentView"))?.textSize = 0F
                        }
                    }
                }
            }

        mBatteryMeterViewClass.methodFinder()
            .filterByName("updateChargeAndText")
            .single().createHook {
                after { param ->
                    if (param.thisObject != null) {
                        // 隐藏电池百分号
                        if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") ||
                            mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")
                        ) {
                            (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentMarkView"))?.textSize = 0F
                        }
                        // 隐藏电池的百分比
                        // Todo：内显百分比暂无法隐藏，因为布局已修改
                        if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                            (param.thisObject?.getObjectFieldAs<TextView>("mBatteryPercentView"))?.textSize = 0F
                        }

                        // 隐藏电池充电图标
                        // Todo：内显闪电图标暂无法隐藏，因为布局已修改
                        if (mPrefsMap.getBoolean("system_ui_status_bar_battery_charging")) {
                            (param.thisObject.getObjectFieldAs<ImageView>("mBatteryChargingView")).visibility =
                                View.GONE
                        }
                    }
                }
            }
    }
}
