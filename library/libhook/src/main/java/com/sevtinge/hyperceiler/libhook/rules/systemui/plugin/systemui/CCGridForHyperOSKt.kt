/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass

// from YunZiA
object CCGridForHyperOSKt {
    private val radius by lazy {
        PrefsBridge.getInt("system_ui_control_center_rounded_rect_radius", 72).toFloat()
    }

    fun initCCGridForHyperOS(classLoader: ClassLoader?) {
        loadClass("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader)
            .beforeHookMethod("setDisabledBg", Drawable::class.java) { param ->
                runCatching {
                    val drawable = param.args[0] as Drawable
                    if (drawable is GradientDrawable) drawable.cornerRadius = radius
                    param.args[0] = drawable
                }.onFailure {
                    XposedLog.e("initCCGridForHyperOS", "radius 1 crash, $it")
                }
            }

        loadClass("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader)
            .beforeHookMethod("setEnabledBg", Drawable::class.java) { param ->
                runCatching {
                    val drawable = param.args[0] as Drawable
                    if (drawable is GradientDrawable) drawable.cornerRadius = radius
                    param.args[0] = drawable
                }.onFailure {
                    XposedLog.e("initCCGridForHyperOS", "radius 2 crash, $it")
                }
            }

        // OS2 加载磁贴时的圆角
        runCatching {
            loadClass("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader)
                .replaceMethod("getCornerRadius") {
                    radius
                }
        }.onFailure {
            XposedLog.e("initCCGridForHyperOS", "radius 4 crash, $it")
        }
    }
}
