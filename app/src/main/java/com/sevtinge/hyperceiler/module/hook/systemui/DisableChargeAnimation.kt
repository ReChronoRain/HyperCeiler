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
package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object DisableChargeAnimation : BaseHook() {
    override fun init() {
        /*loadClass("com.miui.charge.ChargeUtils", lpparam.classLoader).methodFinder().first {
            name == "getChargeAnimationType"
        }.createHook {
            returnConstant(4)
            // 0, 4以上: 光韵
            // 1: 流光
            // 2: 流光 Pad
            // 3: 光韵渐入
        }*/

        loadClass("com.miui.charge.container.MiuiChargeContainerView").methodFinder().first {
            name == "init"
        }.createHook {
            returnConstant(null)
        }
    }
}
