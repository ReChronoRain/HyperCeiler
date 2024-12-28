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

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.sevtinge.hyperceiler.utils.prefs.*
import de.robv.android.xposed.*

// from YunZiA
object CCGridForHyperOSKt {
    private val radius by lazy {
        PrefsUtils.mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72).toFloat() }

    @JvmStatic
    fun initCCGridForHyperOS(classLoader: ClassLoader?) {
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "getActiveBackgroundDrawable", "com.android.systemui.plugins.qs.QSTile\$State", object : XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val drawable = param?.result as Drawable
                if (drawable is GradientDrawable) drawable.cornerRadius = radius
                param.result = drawable
            }
        })
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "getDisabledBackgroundDrawable", "com.android.systemui.plugins.qs.QSTile\$State", object : XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val drawable = param?.result as Drawable
                if (drawable is GradientDrawable) drawable.cornerRadius = radius
                param.result = drawable
            }
        })
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "getBackgroundDrawable", "com.android.systemui.plugins.qs.QSTile\$State", object : XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val state = param?.args?.get(0)
                val i = XposedHelpers.getIntField(state, "state")
                if (i == 0) {
                    val drawable = param?.result as Drawable
                    if (drawable is GradientDrawable) drawable.cornerRadius = radius
                    param.result = drawable
                }
            }
        })
    }
}