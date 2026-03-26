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
package com.sevtinge.hyperceiler.hook.utils.api

import android.content.ComponentName
import android.content.Context
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference

// https://github.com/buffcow/Hyper5GSwitch/blob/master/app/src/main/kotlin/cn/buffcow/hyper5g/hooker/PluginLoader.kt
internal class PluginFactory(obj: Any) {

    companion object {
        const val PLUGIN_COMPONENT_MIUI_SYSTEMUI = "miui.systemui.plugin"
        const val PLUGIN_COMPONENT_MIUI_AOD = "com.miui.aod"
    }

    lateinit var pluginCtxRef: WeakReference<Context>
    val mComponentName: Any? = XposedHelpers.getObjectField(obj , "mComponentName")

    fun componentNames(type: Int, str: String): ComponentName {
        return when (type) {
            0 ->  ComponentName(PLUGIN_COMPONENT_MIUI_AOD, str)
            else ->  ComponentName(PLUGIN_COMPONENT_MIUI_SYSTEMUI, str)
        }
    }
}
