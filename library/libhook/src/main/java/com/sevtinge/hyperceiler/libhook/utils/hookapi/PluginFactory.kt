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
package com.sevtinge.hyperceiler.libhook.utils.hookapi

import android.content.ComponentName
import android.content.Context
import java.lang.ref.WeakReference

// https://github.com/buffcow/Hyper5GSwitch/blob/master/app/src/main/kotlin/cn/buffcow/hyper5g/hooker/PluginLoader.kt
internal class PluginFactory(obj: Any) {

    companion object {
        const val PLUGIN_COMPONENT_MIUI_SYSTEMUI = "miui.systemui.plugin"
        const val PLUGIN_COMPONENT_MIUI_AOD = "com.miui.aod"
    }

    lateinit var pluginCtxRef: WeakReference<Context>

    /**
     * The field in PluginInstance$PluginFactory that holds the plugin's component name.
     *
     * It is called mComponentName on OS 2.x / OS 3.0. HyperOS 3.3 (Android 17) followed
     * AOSP in dropping the m prefix and renamed it to componentName, so reading only the
     * old name throws MemberNotFoundException -- which the caller's runCatching swallows
     * into a bare "Failed to create plugin context.". The result is that every
     * miui.systemui.plugin feature (volume/brightness percentage, QS colors, control
     * center media card, ...) silently stops working. The old name is tried first so
     * behaviour on older versions is unchanged.
     */
    val mComponentName: Any? =
        runCatching {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(obj, "mComponentName")
        }.getOrElse {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(obj, "componentName")
        }

    fun componentNames(type: Int, str: String): ComponentName {
        return when (type) {
            0 ->  ComponentName(PLUGIN_COMPONENT_MIUI_AOD, str)
            else ->  ComponentName(PLUGIN_COMPONENT_MIUI_SYSTEMUI, str)
        }
    }
}
