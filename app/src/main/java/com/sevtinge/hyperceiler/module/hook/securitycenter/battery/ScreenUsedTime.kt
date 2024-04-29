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
package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals

object ScreenUsedTime : BaseHook() {
    private val method1 by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("ishtar", "nuwa", "fuxi")
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }
    private val method2 by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("not support screenPowerSplit", "PowerRankHelperHolder")
                }
                returnType = "boolean"
                // paramTypes = listOf() 2.0.0-rc3 已经修复此错误，可以使用
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        Log.i("methods2 :$method2")
        method2.forEach {
            it.createHook {
                returnConstant(
                    when (it) {
                        method1 -> true
                        else -> false
                    }
                )
            }
        }
    }
}
