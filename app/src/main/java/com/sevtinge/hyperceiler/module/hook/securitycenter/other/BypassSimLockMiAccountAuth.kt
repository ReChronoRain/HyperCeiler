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
package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.*

object BypassSimLockMiAccountAuth : BaseHook() {
    private val findMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingString("SimLockUtils", StringMatchType.Contains)
                }
                addCall {
                    addUsingString("SimLockStartFragment::simLockSetUpFlow::step =", StringMatchType.Contains)
                }
                paramCount = 1
                paramTypes("android.content.Context")
                returnType = "boolean"
            }
        }.map { it.getMethodInstance(lpparam.classLoader) }.toSet()
    }

    override fun init() {
        logD(TAG, lpparam.packageName, "BypassSimLockMiAccountAuth find method is ${findMethod.last()}")
        findMethod.last().createHook {
            returnConstant(true)
        }
    }
}
