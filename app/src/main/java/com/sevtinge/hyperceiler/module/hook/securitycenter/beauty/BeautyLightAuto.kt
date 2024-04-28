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
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import de.robv.android.xposed.*
import java.lang.reflect.*

object BeautyLightAuto : BaseHook() {
    private val beauty by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("taoyao", "IN", "persist.vendor.vcb.ability")
                returnType = "boolean"
            }
        }.single().getMethodInstance(lpparam.classLoader)
    }
    private val beautyAuto by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingStringsEquals("taoyao")
                returnType = "boolean"
            }
        }
    }

    override fun init() {
        if (mPrefsMap.getBoolean("security_center_beauty_face")) {
            beauty.createHook {
                returnConstant(true)
            }
        }

        beautyAuto.forEach {
            if (!java.lang.String.valueOf(it).contains("<clinit>")) {
                val beautyLightAuto: Method =
                    it.getMethodInstance(lpparam.classLoader)
                if (!java.lang.String.valueOf(it).contains(beauty.toString()) && beautyLightAuto.name != beauty.name) {
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
