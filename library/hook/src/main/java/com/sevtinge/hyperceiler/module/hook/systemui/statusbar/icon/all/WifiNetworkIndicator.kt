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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.getStaticObjectField
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XposedHelpers

object WifiNetworkIndicator : BaseHook() {
    private val mStatusBarWifiViewNew by lazy {
        findClassIfExists("com.android.systemui.statusbar.pipeline.wifi.data.repository.prod.WifiRepositoryImpl")
    }
    private val mStatusBarWifiView by lazy {
        findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView")
    }

    override fun init() {
        if (isMoreHyperOSVersion(2f)) {
            hideWifiActivityNew()
        } else {
            hideWifiActivity()
        }

    }

    private fun hideWifiActivityNew() {
        hookAllConstructors(mStatusBarWifiViewNew, object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
            override fun after(param: MethodHookParam) {
                param.thisObject.setObjectField(
                    "wifiActivity",
                    newReadonlyStateFlow(
                        mStatusBarWifiViewNew.getStaticObjectField("ACTIVITY_DEFAULT")
                    )
                )
            }
        })
    }

    private fun hideWifiActivity() {
        _root_ide_package_.com.sevtinge.hyperceiler.module.base.tool.HookTool.hookAllMethods(
            mStatusBarWifiView,
            "applyWifiState",
            object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
                override fun after(param: MethodHookParam) {
                    val mWifiActivityView =
                        param.thisObject.getObjectField("mWifiActivityView")
                    XposedHelpers.callMethod(mWifiActivityView, "setVisibility", View.GONE)
                }
            })
    }
}
