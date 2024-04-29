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
package com.sevtinge.hyperceiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals

object DisableAD : BaseHook() {
    override fun init() {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("ads_enable")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }

        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("app_store_recommend")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }

        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("virus_scan_install")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }
    }
}
