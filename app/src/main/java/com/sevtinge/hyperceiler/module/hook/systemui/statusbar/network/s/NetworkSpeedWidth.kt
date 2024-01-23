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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.s

import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object NetworkSpeedWidth : BaseHook() {
    override fun init() {
        // 固定宽度以防相邻元素左右防抖
        if (mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10) > 10) {
            hookAllMethods(
                "com.android.systemui.statusbar.views.NetworkSpeedView",
                lpparam.classLoader,
                "applyNetworkSpeedState",
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val meter = param.thisObject as TextView
                        if (meter.tag == null || "slot_text_icon" != meter.tag) {
                            XposedHelpers.getAdditionalInstanceField(param.thisObject, "inited")
                        }
                    }
                }
            )
        }
    }
}
