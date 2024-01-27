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

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object WifiNetworkIndicator : BaseHook() {
    var mVisibility = 0
    private var mStatusBarWifiView: Class<*>? = null
    override fun init() {
        mStatusBarWifiView = findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView")
        val mWifiNetworkIndicator =
            mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0)

        when (mWifiNetworkIndicator) {
            1 -> mVisibility = View.VISIBLE
            2 -> mVisibility = View.INVISIBLE
        }

        val hideWifiActivity: MethodHook = object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val mWifiActivityView =
                    XposedHelpers.getObjectField(param.thisObject, "mWifiActivityView")
                XposedHelpers.callMethod(mWifiActivityView, "setVisibility", mVisibility)
            }
        }
        hookAllMethods(mStatusBarWifiView, "applyWifiState", hideWifiActivity)
    }
}
