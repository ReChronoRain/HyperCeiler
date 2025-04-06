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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.*

object ScreenUsedTime : BaseHook() {
    private val method1 by lazy<Method> {
        DexKit.findMember("ScreenUsedTime1") {
            it.findMethod {
                matcher {
                    usingEqStrings("ishtar", "nuwa", "fuxi")
                    returnType = "boolean"
                    paramCount = 0
                }
            }.single()
        }
    }
    private val method2 by lazy<List<Method>> {
        DexKit.findMemberList("ScreenUsedTime2") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingEqStrings("not support screenPowerSplit", "PowerRankHelperHolder")
                    }
                    returnType = "boolean"
                    paramCount = 0
                }
            }
        }
    }

    override fun init() {
        logD(TAG, lpparam.packageName, "methods2 :$method2")
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
