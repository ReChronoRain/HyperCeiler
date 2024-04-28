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

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*

object DisableRemoveScreenHoldOn : BaseHook() {
    private val screen by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addEqString("remove_screen_off_hold_on")
                returnType = "boolean"
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    override fun init() {
        screen.createHook {
            returnConstant(false)
        }
    }
}
