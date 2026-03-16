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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.content.Context
import android.content.res.Configuration
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField

object AlwaysDark : BaseHook() {
    private val backgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0)
    }
    private val alwaysDark by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_always_dark")
    }

    override fun init() {
        if (backgroundStyle != 0 || !alwaysDark) return

        val fldListeners = findClassIfExists(
            "com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController"
        )?.findFieldOrNull("mListeners")

        val metLazyGet = findClassIfExists("dagger.Lazy")
            ?.declaredMethods?.firstOrNull { it.name == "get" }

        miuiMediaViewControllerImpl?.let { clz ->
            clz.afterHookConstructor { param ->
                val context = param.thisObject.getObjectFieldOrNullAs<Context>("context")
                    ?: return@afterHookConstructor
                val oriConfig = context.resources.configuration
                val newConfig = Configuration(oriConfig).apply {
                    uiMode = (oriConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        Configuration.UI_MODE_NIGHT_YES
                }
                param.thisObject.setObjectField("context", context.createConfigurationContext(newConfig))
            }

            clz.afterHookMethod("attach") { param ->
                val listener = param.thisObject.getObjectFieldOrNull("mediaFullAodListener") ?: return@afterHookMethod
                val controller = param.thisObject.getObjectFieldOrNull("fullAodController")
                    ?.let { metLazyGet?.invoke(it) } ?: return@afterHookMethod
                (fldListeners?.get(controller) as? MutableList<*>)?.remove(listener)
            }
        }
    }
}
