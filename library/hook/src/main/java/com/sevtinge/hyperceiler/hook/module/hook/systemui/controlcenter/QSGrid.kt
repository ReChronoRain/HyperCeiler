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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter

import android.content.res.Configuration
import android.view.ViewGroup
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

class QSGrid : BaseHook() {
    private val miuiTileClass by lazy {
        loadClass("com.android.systemui.qs.MiuiTileLayout")
    }

    override fun init() {
        val cols = mPrefsMap.getInt("system_control_center_old_qs_columns", 4)
        val colsHorizontal = mPrefsMap.getInt("system_control_center_old_qs_columns_horizontal", 5)
        val rows = mPrefsMap.getInt("system_control_center_old_qs_rows", 3)
        val rowsHorizontal = mPrefsMap.getInt("system_control_center_old_qs_rows_horizontal", 2)

        hyperHooks(cols, colsHorizontal)
        miuiTileClass.methodFinder()
            .filterByName("updateResources")
            .first().createAfterHook {
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
        miuiTileClass.methodFinder()
            .filterByName("layoutTileRecords")
            .filterByParamCount(1)
            .first().createAfterHook {
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
