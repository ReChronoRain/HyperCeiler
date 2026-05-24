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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.game

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.lang.reflect.Method

class RemoveMacroBlackList : BaseHook() {
    override fun useDexKit() = true
    private lateinit var removeMacroBlackListMethod1: Method
    private lateinit var removeMacroBlackListMethod2: Method
    private lateinit var removeMacroBlackListClass3: Class<*>

    override fun initDexKit(): Boolean {
        removeMacroBlackListMethod1 = requiredMember("RemoveMacroBlackList1") {
            it.findMethod {
                matcher {
                    addEqString("pref_gb_unsupport_macro_apps")
                    paramCount = 0
                }
            }.single()
        }
        removeMacroBlackListMethod2 = requiredMember("RemoveMacroBlackList2") {
            it.findMethod {
                matcher {
                    returnType = "boolean"
                    addInvoke {
                        addEqString("pref_gb_unsupport_macro_apps")
                        paramCount = 0
                    }
                }
            }.single()
        }
        removeMacroBlackListClass3 = requiredMember("RemoveMacroBlackList3") {
            it.findClass {
                matcher {
                    usingStrings =
                        listOf("content://com.xiaomi.macro.MacroStatusProvider/game_macro_change")
                }
            }.single()
        }
        return true
    }

    override fun init() {
        removeMacroBlackListMethod1.createHook {
            returnConstant(ArrayList<String>())
        }

        removeMacroBlackListMethod2.createHook {
            returnConstant(false)
        }

        removeMacroBlackListClass3.apply {
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
