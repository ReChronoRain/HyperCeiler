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
package com.sevtinge.hyperceiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import java.lang.reflect.Method

object BeautyFace : BaseHook() {
    var beautyFace: Method? = null
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("taoyao", "IN", "persist.vendor.vcb.ability")
                returnType = "boolean"
            }
        }.forEach {
            beautyFace = it.getMethodInstance(lpparam.classLoader)
        }

        beautyFace!!.createHook {
            returnConstant(true)
        }
    }
}
