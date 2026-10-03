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
    // Android 17 moved the service into its own package and renamed the dispatch method.
    private const val SERVICE_OLD = "com.android.server.power.ThermalManagerService"
    private const val SERVICE_NEW = "com.android.server.power.thermal.ThermalManagerService"

    override fun init() {
        val service = findClassIfExists(SERVICE_NEW) ?: findClassIfExists(SERVICE_OLD) ?: return
        service.findAllMethods { name("postEventListener") }
            .createBeforeHooks {
                it.result = null
            }
        service.findAllMethods { name("postEventListenerLocked") }
            .createBeforeHooks {
                it.result = null
            }
    }
}
