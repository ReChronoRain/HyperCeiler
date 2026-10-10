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
        // OS4: com.android.server.power.ThermalManagerService 已随 ROM 重构移除（全索引无 postEventListener 属主）。
        // 类存在时按原挂点禁热管理；不存在时直接跳过（ThermalObserver.updateTemperature 由 ThermalBrightness 规则覆盖）。
        val thermalService = findClassIfExists("com.android.server.power.ThermalManagerService") ?: return
        thermalService.findAllMethods { name("postEventListener") }
            .createBeforeHooks {
                it.result = null
            }

    }
}
