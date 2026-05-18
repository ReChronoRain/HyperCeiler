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
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

class QSGrid : BaseHook() {
    private val miuiTileClass by lazy {
        loadClass("com.android.systemui.qs.MiuiTileLayout")
    }

    override fun init() {
        val cols = PrefsBridge.getInt("system_control_center_old_qs_columns", 4)
        val colsHorizontal = PrefsBridge.getInt("system_control_center_old_qs_columns_horizontal", 5)
        val rows = PrefsBridge.getInt("system_control_center_old_qs_rows", 3)
        val rowsHorizontal = PrefsBridge.getInt("system_control_center_old_qs_rows_horizontal", 2)

        hyperHooks(cols, colsHorizontal)
        miuiTileClass.findMethod { name("updateResources") }.createAfterHook {
                val viewGroup = it.thisObject as ViewGroup
                val mConfiguration: Configuration = viewGroup.context.resources.configuration
                if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    it.thisObject.setObjectField("mMaxAllowedRows", rows)
                } else {
                    it.thisObject.setObjectField("mMaxAllowedRows", rowsHorizontal)
                }
                viewGroup.requestLayout()
            }
    }

    private fun hyperHooks(
        cols: Int,
        colsHorizontal: Int
    ) {
        miuiTileClass.findMethod { name("layoutTileRecords"); paramCount(1) }.createAfterHook {
                val viewGroup = it.thisObject as ViewGroup
                val mConfiguration: Configuration = viewGroup.context.resources.configuration
                if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    it.thisObject.setObjectField("mColumns", cols)
                } else {
                    it.thisObject.setObjectField("mColumns", colsHorizontal)
                }
            }
    }
}
