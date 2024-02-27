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

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge

object UnlockEnhanceContours : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("ro.vendor.media.video.frc.support")
            }
        }.forEach {
            val qaq = it.getClassInstance(EzXHelper.classLoader)
            var counter = 0
            dexKitBridge.findMethod {
                matcher {
                    declaredClass = qaq.name
                    returnType = "boolean"
                    paramTypes = listOf("java.lang.String")
                }
            }.forEach { methods ->
                counter++
                if (counter == 3) {
                    methods.getMethodInstance(EzXHelper.classLoader).createHook {
                        returnConstant(true)
                    }
                }
            }
            val tat = dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("debug.config.media.video.ais.support")
                    declaredClass = qaq.name
                }
            }.single().getMethodInstance(EzXHelper.classLoader)
            val newChar = tat.name.toCharArray()
            for (i in newChar.indices) {
                newChar[i]++
            }
            val newName = String(newChar)
            tat.declaringClass.methodFinder()
                .filterByName(newName)
                .first().createHook {
                    returnConstant(true)
                }
        }
    }
}
