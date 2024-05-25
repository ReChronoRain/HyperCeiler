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
package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod

object UnlockSuperWirelessCharge : BaseHook() {

    private val superWirelessCharge by lazy {
        DexKit.getDexKitBridge("superWirelessCharge") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("persist.vendor.tx.speed.control")
                    returnType = "boolean"
                }
            }.single().getMethodInstance(EzXHelper.classLoader)
        }.toMethod()
    }

    private val superWirelessChargeTip by lazy {
        DexKit.getDexKitBridge("superWirelessChargeTip") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("key_is_connected_super_wls_tx")
                    returnType = "boolean"
                }
            }.single().getMethodInstance(EzXHelper.classLoader)
        }.toMethod()
    }

    override fun init() {
        superWirelessCharge.createHook {
            returnConstant(true)
        }

        superWirelessChargeTip.createHook {
            returnConstant(true)
        }
    }
}
