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
package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import java.lang.reflect.*

object UnlockDisney : BaseHook() {
    private val mickey by lazy<Method> {
        DexKit.findMember("UnlockDisneyMickey") {
            it.findMethod {
                matcher {
                    addCaller {
                        usingStrings("magic_recycler_matting_0", "magic_recycler_clear_icon")
                        // returnType = "java.util.List" // 你米 1.6.5.10.2 改成了 java.util.ArrayList，所以找不到
                        paramCount = 0
                    }
                    modifiers = Modifier.STATIC
                    returnType = "boolean"
                    paramCount = 0
                }
            }.single()
        }
    }

    private val bear by lazy<Method> {
        DexKit.findMember("UnlockDisneyBear") {
            it.findMethod {
                matcher {
                    declaredClass = mickey.declaringClass.name
                    modifiers = Modifier.STATIC
                    returnType = "boolean"
                    paramCount = 0
                }
            }.last()
        }
    }

    private val isType by lazy {
        mPrefsMap.getStringAsInt("mediaeditor_unlock_disney_some_func", 0)
    }

    override fun init() {
        when (isType) {
            1 -> {
                isHook(mickey, true)
                isHook(bear, false)
            }
            2 -> {
                isHook(mickey, false)
                isHook(bear, true)
            }
        }
    }

    private fun isHook(method: Method, bool: Boolean) {
        method.createHook {
            returnConstant(bool)
        }
    }
}
