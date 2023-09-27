package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter

import android.content.res.Configuration
import android.view.ViewGroup

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder

import com.sevtinge.cemiuiler.module.base.BaseHook

class QQSGrid : BaseHook() {
    override fun init() {
        val cols = mPrefsMap.getInt("system_control_center_old_qs_grid_columns", 5);
        val colsHorizontal = mPrefsMap.getInt("system_control_center_old_qs_grid_columns_horizontal", 6);

        loadClass("com.android.systemui.qs.MiuiQuickQSPanel").methodFinder().first {
                name == "setMaxTiles" && parameterCount == 1
            }.createHook {
                before {
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
}
