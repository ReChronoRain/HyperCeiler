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

import android.content.*
import android.graphics.drawable.*
import com.sevtinge.hyperceiler.module.base.tool.HookTool.*
import com.sevtinge.hyperceiler.utils.log.*
import com.sevtinge.hyperceiler.utils.prefs.*
import de.robv.android.xposed.*

object CCGridForHyperOS {
    private val radius by lazy {
        PrefsUtils.mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72).toFloat() }

    @JvmStatic
    fun initCCGridForHyperOS(classLoader: ClassLoader?) {
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "setCornerRadius", Float::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.args[0] = radius
                }
            }
        )

        var warningD: Drawable? = null
        var enabledD: Drawable? = null
        var restrictedD: Drawable? = null
        var disabledD: Drawable? = null
        var unavailableD: Drawable? = null

        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "updateIcon", "com.android.systemui.plugins.qs.QSTile\$State", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val pluginContext: Context =
                        XposedHelpers.getObjectField(param?.thisObject, "pluginContext") as Context;
                    if (warningD == null) {
                        val warning: Int = pluginContext.resources.getIdentifier("qs_background_warning", "drawable", "miui.systemui.plugin")
                        warningD = pluginContext.theme.getDrawable(warning);
                    }
                    if (enabledD == null) {
                        val enabled: Int = pluginContext.resources.getIdentifier("qs_background_enabled", "drawable", "miui.systemui.plugin")
                        enabledD = pluginContext.theme.getDrawable(enabled);
                    }
                    if (restrictedD == null) {
                        val restricted: Int = pluginContext.resources.getIdentifier("qs_background_restricted", "drawable", "miui.systemui.plugin")
                        restrictedD = pluginContext.theme.getDrawable(restricted);
                    }
                    if (disabledD == null) {
                        val disabled: Int = pluginContext.resources.getIdentifier("qs_background_disabled", "drawable", "miui.systemui.plugin")
                        disabledD = pluginContext.theme.getDrawable(disabled);
                    }
                    if (unavailableD == null) {
                        val unavailable: Int = pluginContext.resources.getIdentifier("qs_background_unavailable", "drawable", "miui.systemui.plugin")
                        unavailableD = pluginContext.theme.getDrawable(unavailable);
                    }
                    if (warningD is GradientDrawable) {
                        (warningD as GradientDrawable).cornerRadius = radius
                    }
                    if (enabledD is GradientDrawable) {
                        (enabledD as GradientDrawable).cornerRadius = radius
                    }
                    if (restrictedD is GradientDrawable) {
                        (restrictedD as GradientDrawable).cornerRadius = radius
                    }
                    if (disabledD is GradientDrawable) {
                        (disabledD as GradientDrawable).cornerRadius = radius
                    }
                    if (unavailableD is GradientDrawable) {
                        (unavailableD as GradientDrawable).cornerRadius = radius
                    }
                }
            }
        )
    }
}
