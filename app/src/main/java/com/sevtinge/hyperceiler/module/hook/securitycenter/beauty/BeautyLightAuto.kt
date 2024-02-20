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

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge

object BeautyLightAuto : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("taoyao")
                returnType = "boolean"
            }
        }.forEach {
            if (!java.lang.String.valueOf(it).contains("<clinit>")) {
                val beautyLightAuto: java.lang.reflect.Method =
                    it.getMethodInstance(lpparam.classLoader)
                if (!java.lang.String.valueOf(it).contains(BeautyFace.beautyFace.toString())) {
                    logI(
                        TAG,
                        this.lpparam.packageName,
                        "beautyLightAuto method is $beautyLightAuto"
                    )
                    XposedBridge.hookMethod(
                        beautyLightAuto,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        }
    }
}
