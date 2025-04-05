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
package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import java.lang.reflect.*

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        DexKit.findMember<Method>("RemoveMacroBlackList1") {
            it.findMethod {
                matcher {
                    addEqString("pref_gb_unsupport_macro_apps")
                    paramCount = 0
                }
            }.single()
        }.createHook {
            returnConstant(ArrayList<String>())
        }

        DexKit.findMember<Method>("RemoveMacroBlackList2") {
            it.findMethod {
                matcher {
                    returnType = "boolean"
                    addInvoke {
                        addEqString("pref_gb_unsupport_macro_apps")
                        paramCount = 0
                    }
                }
            }.single()
        }.createHook {
            returnConstant(false)
        }

        DexKit.findMember<Class<*>>("RemoveMacroBlackList3") {
            it.findClass {
                matcher {
                    usingStrings =
                        listOf("content://com.xiaomi.macro.MacroStatusProvider/game_macro_change")
                }
            }.single()
        }.apply {
            methodFinder().filterByParamCount(2)
                .toList().createHooks {
                    returnConstant(true)
                }
            methodFinder().filterByParamCount(3)
                .toList().createHooks {
                    returnConstant(true)
                }
        }
    }
}
