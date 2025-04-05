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
package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.provider.Settings
import com.sevtinge.hyperceiler.module.base.BaseHook

object NoLowBatteryWarning : BaseHook() {
    override fun init() {
        val settingHook: MethodHook = object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
            override fun before(param: MethodHookParam) {
                val key = param.args[1] as String
                if ("low_battery_dialog_disabled" == key) param.result = 1
                else if ("low_battery_sound" == key) param.result = null
            }
        }
        _root_ide_package_.com.sevtinge.hyperceiler.module.base.tool.HookTool.hookAllMethods(
            Settings.System::class.java,
            "getInt",
            settingHook
        )
        _root_ide_package_.com.sevtinge.hyperceiler.module.base.tool.HookTool.hookAllMethods(
            Settings.Global::class.java,
            "getString",
            settingHook
        )
    }
}
