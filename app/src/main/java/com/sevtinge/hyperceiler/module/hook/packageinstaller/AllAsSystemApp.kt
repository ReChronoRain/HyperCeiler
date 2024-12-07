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
package com.sevtinge.hyperceiler.module.hook.packageinstaller

import android.content.pm.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*

object AllAsSystemApp : BaseHook() {
    private val systemMethod by lazy {
        DexKit.getDexKitBridgeList("AllAsSystemApp") {
            it.findMethod {
                matcher {
                    paramTypes = listOf("android.content.pm.ApplicationInfo")
                    returnType = "boolean"
                }
            }.toElementList()
        }.toMethodList()
    }

    override fun init() {
        systemMethod.createHooks {
            before { param ->
                (param.args[0] as ApplicationInfo).flags =
                    (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
            }
        }
    }
}
