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
package com.sevtinge.hyperceiler.module.hook.milink

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import java.lang.reflect.*

object NewUnlockHMind : BaseHook() {
    private val unlockHMind by lazy {
        // 解锁 Xiaomi HyperMind
        // 适配 15.x.x.x ~ 16.x.x.x
        // 这要是坏了，除非动了 cetus 字符串，否则不可能会炸
        // 哦对了，我说怎么平板不配，原来 TMD 把横屏适配删了，米米你啥时候加回来 QAQ！
        DexKit.findMember("NewHMindManager") { dexkit ->
            dexkit.findMethod {
                matcher {
                    addCaller {
                        declaredClass = "com.miui.circulate.world.CirculateWorldActivity"
                        usingStrings("cetus")
                    }
                    declaredClass = "com.milink.hmindlib.HMindManager"
                    returnType = "boolean"
                    modifiers = Modifier.FINAL
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    override fun init() {
        unlockHMind.createHook {
            returnConstant(true)
        }
    }
}