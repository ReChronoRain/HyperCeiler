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
package com.sevtinge.hyperceiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.isFinal
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import java.lang.reflect.*

object CustomRefreshRate : BaseHook() {
    private val resultMethod by lazy<Method> {
        DexKit.findMember("CustomRefreshRate") {
            it.findMethod {
                matcher {
                    addEqString("btn_preferce_category")
                }
            }.single()
        }
    }
    override fun init() {
        val resultClass = loadClass("com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity")

        resultMethod.createHook {
            before {
                it.args[0] = true
            }
        }

        resultClass.declaredFields.first { field ->
            field.isFinal && field.isStatic
        }.apply { isAccessible = true }.set(null, true)
    }
}
