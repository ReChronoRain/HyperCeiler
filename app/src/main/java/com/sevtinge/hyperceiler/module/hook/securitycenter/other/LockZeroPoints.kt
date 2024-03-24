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

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object LockZeroPoints : BaseHook() {
    private val score by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingString("getMinusPredictScore", StringMatchType.Contains)
                }
                usingNumbers(41, 100, 0)
                returnType = "int"
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    private val scoreOld by lazy {
        dexKitBridge.findMethod {
            matcher {
                 addUsingString("getMinusPredictScore", StringMatchType.Contains)
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").methodFinder()
            .filterByName("onClick")
            .filterByParamTypes(View::class.java)
            .first().createHook {
               returnConstant(null)
            }

        logI(TAG, lpparam.packageName, "LockZeroPoints method is $scoreOld and $score")
        score.createHook {
            replace { 0 }
        }
        scoreOld.createHook {
            replace { 100 }
        }
    }
}
