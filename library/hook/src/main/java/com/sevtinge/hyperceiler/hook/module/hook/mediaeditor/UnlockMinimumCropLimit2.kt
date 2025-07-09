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
package com.sevtinge.hyperceiler.hook.module.hook.mediaeditor

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object UnlockMinimumCropLimit2 : BaseHook() {
    override fun init() {
        DexKit.findMemberList<Method>("MinimumCropLimit2") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "int"
                    paramCount = 0
                    usingNumbers(0.5f) // 1.8 开始有一个方法去除了 200 这个参数，需要重找
                    modifiers = Modifier.FINAL

                    // 理论上适配 1.7 - 1.8+ 全版本
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
