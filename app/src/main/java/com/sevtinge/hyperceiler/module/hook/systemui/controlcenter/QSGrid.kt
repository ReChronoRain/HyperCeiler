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
