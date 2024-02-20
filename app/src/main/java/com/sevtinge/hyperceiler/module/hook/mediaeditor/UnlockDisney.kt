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

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockDisney : BaseHook() {
    override fun init() {
        val disney = dexKitBridge.findMethod {
            matcher {
                addCall {
                    addUsingStringsEquals("magic_recycler_matting_0", "magic_recycler_clear_icon")
                    returnType = "java.util.List"
                    paramCount = 0
                }
                modifiers = Modifier.STATIC
                returnType = "boolean"
                paramCount = 0
            }
        }.single().getMethodInstance(safeClassLoader)

        // debug 用
        logI(TAG,"disney name is $disney")
        disney.createHook {
            returnConstant(true)
        }
    }
}
