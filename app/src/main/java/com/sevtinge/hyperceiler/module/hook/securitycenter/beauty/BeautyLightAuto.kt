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

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toElementList
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import de.robv.android.xposed.*

object BeautyLightAuto : BaseHook() {
    private val beauty by lazy {
        DexKit.getDexKitBridge("superWirelessCharge") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("taoyao")
                    returnType = "boolean"
                }
            }.single().getMethodInstance(EzXHelper.classLoader)
        }.toMethod()
    }
    private val beautyAuto by lazy {
        DexKit.getDexKitBridgeList("superWirelessCharge") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("taoyao")
                    returnType = "boolean"
                }
            }.toElementList(EzXHelper.classLoader)
        }.toMethodList()
    }

    override fun init() {
        if (mPrefsMap.getBoolean("security_center_beauty_face")) {
            beauty.createHook {
                returnConstant(true)
            }
        }

        beautyAuto.forEach {
            if (!java.lang.String.valueOf(it).contains("<clinit>")) {
                if (!java.lang.String.valueOf(it.name).contains(beauty.toString()) && it.name != beauty.name) {
                    logI(TAG, this.lpparam.packageName, "beautyLightAuto method is $beautyAuto")
                    XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(true))
                }
            }
        }
    }
}
