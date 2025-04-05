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
package com.sevtinge.hyperceiler.module.hook.screenshot

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import java.lang.reflect.*

object UnlockMinimumCropLimit2 : BaseHook() {
    override fun init() {
        DexKit.findMemberList<Method>("MinimumCropLimit2") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "int"
                    paramCount = 0
                    usingNumbers(0.5f) // 你滴大大的坏，和相册编辑一样删除了 200 这个数字
                    modifiers = Modifier.PRIVATE

                    addInvoke("Ljava/lang/Math;->max(II)I")
                }
            }
        }.forEach { method ->
            method.createHook {
                returnConstant(0)
            }
        }
    }
}
