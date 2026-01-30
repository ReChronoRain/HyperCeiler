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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getStaticObjectField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField

object WifiNetworkIndicator : BaseHook() {
    private val mStatusBarWifiViewNew by lazy {
        findClassIfExists("com.android.systemui.statusbar.pipeline.wifi.data.repository.prod.WifiRepositoryImpl")
    }

    override fun init() {
        mStatusBarWifiViewNew.hookAllConstructors {
            after {
                it.thisObject.setObjectField(
                    "wifiActivity",
                    newReadonlyStateFlow(
                        mStatusBarWifiViewNew.getStaticObjectField("ACTIVITY_DEFAULT")
                    )
                )
            }
        }
    }
}
