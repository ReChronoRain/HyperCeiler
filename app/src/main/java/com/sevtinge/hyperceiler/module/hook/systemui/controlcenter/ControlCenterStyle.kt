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

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object ControlCenterStyle : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl").declaredConstructors.createHooks {
            after {
                setObject(it.thisObject, "forceUseControlCenterPanel", false)
            }
        }
        loadClass("com.miui.systemui.SettingsObserver").methodFinder()
            .filterByName("setValue\$default").first()
            .createHook {
                before {
                    if (it.args[1] == "force_use_control_panel") {
                        it.args[2] = 0
                    }
                }
            }
    }
}
