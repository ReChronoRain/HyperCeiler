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
package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.view.*
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import org.luckypray.dexkit.query.enums.*

object LockOneHundredPoints : BaseHook() {
    private val score by lazy {
        DexKit.getDexKitBridge("LockOneHundredPoints1") {
            it.findMethod {
                matcher {
                    declaredClass {
                        addUsingString("getMinusPredictScore", StringMatchType.Contains)
                    }
                    usingNumbers(41, 100, 0)
                    returnType = "int"
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val scoreOld by lazy {
        DexKit.getDexKitBridge("LockOneHundredPoints2") {
            it.findMethod {
                matcher {
                    addUsingString("getMinusPredictScore", StringMatchType.Contains)
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").methodFinder()
            .filterByName("onClick")
            .filterByParamTypes(View::class.java)
            .first().createHook {
               returnConstant(null)
            }

        try {
            logI(TAG, lpparam.packageName, "LockOneHundredPoints method is $score")
            score.createHook {
                replace { 100 }
            }
        } catch (e: Exception) {
            logE(TAG, lpparam.packageName, "LockOneHundredPoints hook Failed: ${e.message}")
            logI(TAG, lpparam.packageName, "LockOneHundredPoints old method is $scoreOld")
            scoreOld.createHook {
                replace { 0 }
            }
        }
    }
}
