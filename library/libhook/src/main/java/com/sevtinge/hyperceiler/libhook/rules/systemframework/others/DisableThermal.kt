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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks

object DisableThermal : BaseHook() {
    override fun init() {
        // OS4: ThermalManagerService 迁到 com.android.server.power.thermal 子包（ROM DEX 核验：
        // 4.0.0.44/4.0.5.0 只有 thermal 子包，OS3.0.319 只有旧包名）。
        // 三版都只存在 postEventListenerCurrentTemperaturesLocked(IThermalEventListener, Integer)；
        // 原先挂的 postEventListener 在任何一版里都不是方法名（ezhook 的 name() 是精确匹配），
        // 所以旧写法从未真正绑定过任何回调。这里改挂真实存在的名字。
        val thermalService = findClassIfExists("com.android.server.power.thermal.ThermalManagerService")
            ?: findClassIfExists("com.android.server.power.ThermalManagerService") ?: return
        thermalService.findAllMethods { name("postEventListenerCurrentTemperaturesLocked") }
            .createBeforeHooks {
                it.result = null
            }

    }
}
