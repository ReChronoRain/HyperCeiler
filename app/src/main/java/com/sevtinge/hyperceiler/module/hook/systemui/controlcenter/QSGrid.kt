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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder

import com.sevtinge.hyperceiler.module.base.BaseHook

import de.robv.android.xposed.XposedHelpers

class QSGrid : BaseHook() {
    override fun init() {
        val cols = mPrefsMap.getInt("system_control_center_old_qs_columns", 4)
        val colsHorizontal = mPrefsMap.getInt("system_control_center_old_qs_columns_horizontal", 5)
        val rows = mPrefsMap.getInt("system_control_center_old_qs_rows", 3)
        val rowsHorizontal = mPrefsMap.getInt("system_control_center_old_qs_rows_horizontal", 2)

        loadClass("com.android.systemui.qs.MiuiTileLayout").methodFinder().first {
                name == "updateColumns"
            }.createHook {
                after {
                    val viewGroup = it.thisObject as ViewGroup
                    val mConfiguration: Configuration = viewGroup.context.resources.configuration
                    if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        XposedHelpers.setObjectField (
                            it.thisObject,
                            "mColumns",
                            cols
                        )
                    } else {
                        XposedHelpers.setObjectField (
                            it.thisObject,
                            "mColumns",
                            colsHorizontal
                        )
                    }
                }
            }

        loadClass("com.android.systemui.qs.MiuiTileLayout").methodFinder().first {
                name == "updateResources"
            }.createHook {
                after {
                    val viewGroup = it.thisObject as ViewGroup
                    val mConfiguration: Configuration = viewGroup.context.resources.configuration
                    if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        XposedHelpers.setObjectField (
                            it.thisObject,
                            "mMaxAllowedRows",
                            rows
                        )
                    } else {
                        XposedHelpers.setObjectField (
                            it.thisObject,
                            "mMaxAllowedRows",
                            rowsHorizontal
                        )
                    }
                    viewGroup.requestLayout()
                }
            }
    }
}
