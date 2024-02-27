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
package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object UnlockMemc : BaseHook() {
    val findFrc by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingString("ro.vendor.media.video.frc.support", StringMatchType.Equals)
                }
                returnType = "boolean"
                paramTypes("java.lang.String")
            }
        }
    }

    override fun init() {
        // 简单拿相册编辑的方法套用一下，后续优化合并同类查找项
        val findFrcMethod = findFrc.filter { methodData ->
            methodData.usingFields.any {
                it.field.typeName == "java.util.List"
            }
        }
        val orderedA = findFrc.map { it.getMethodInstance(classLoader) }.toSet()
        val orderedB = findFrcMethod.map { it.getMethodInstance(classLoader) }.toSet()
        val differentItems = orderedA.subtract(orderedB)

        differentItems.forEach {
            logI(TAG, "find Frc Method is $it")

            it.createHook {
                returnConstant(true)
            }
        }
    }
}
