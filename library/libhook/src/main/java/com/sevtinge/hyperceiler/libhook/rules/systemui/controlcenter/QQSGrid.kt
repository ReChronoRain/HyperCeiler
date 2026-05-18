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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.content.res.Configuration
import android.view.ViewGroup
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

class QQSGrid : BaseHook() {
    override fun init() {
        val cols = PrefsBridge.getInt("system_control_center_old_qs_grid_columns", 5)
        val colsHorizontal = PrefsBridge.getInt("system_control_center_old_qs_grid_columns_horizontal", 6)

        loadClass("com.android.systemui.qs.MiuiQuickQSPanel").findMethod { name("setMaxTiles"); paramCount(1) }.createBeforeHook {
                val viewGroup = it.thisObject as ViewGroup
                val mConfiguration: Configuration = viewGroup.context.resources.configuration
                if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    it.args[0] = cols
                } else {
                    it.args[0] = colsHorizontal
                }
            }
    }
}
