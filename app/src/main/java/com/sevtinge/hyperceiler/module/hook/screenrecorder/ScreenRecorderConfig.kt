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
package com.sevtinge.hyperceiler.module.hook.screenrecorder

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.*

object ScreenRecorderConfig : BaseHook() {
    override fun init() {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("Error when set frame value, maxValue = ")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            before { param ->
                param.args[0] = 1200
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                            fieldAccessible.get(null).let {
                                runCatching {
                                    (it as IntArray).contentEquals(
                                        intArrayOf(15, 24, 30, 48, 60, 90)
                                    )
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
            }
        }

        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("defaultBitRate = ")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            before { param ->
                param.args[0] = 1200
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                            fieldAccessible.get(null).let {
                                runCatching {
                                    (it as IntArray).contentEquals(
                                        intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1)
                                    )
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
            }
        }
    }
}
