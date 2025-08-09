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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.other

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object BypassSimLockMiAccountAuth : BaseHook() {
    private val findMethod by lazy<List<Method>> {
        DexKit.findMemberList("BypassSimLockMiAccountAuth") {
            it.findClass {
                matcher {
                    addUsingString("SimLockUtils", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    addCaller {
                        addUsingString("SimLockStartFragment::simLockSetUpFlow::step =", StringMatchType.Contains)
                    }
                    paramTypes("android.content.Context")
                    returnType = "boolean"
                }
            }
        }
    }

    override fun init() {
        findMethod.last().createHook {
            returnConstant(true)
        }
    }
}
