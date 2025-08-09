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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.other

import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object LockOneHundredPoints : BaseHook() {
    private val score by lazy<Method> {
        DexKit.findMember("LockOneHundredPoints1") {
            it.findClass {
                matcher {
                    className = "com.miui.securityscan.scanner.ScoreManager"
                }
            }.findMethod {
                matcher {
                    addUsingString("getMinusPredictScore", StringMatchType.Contains)
                    returnType = "int"
                }
            }.single()
        }
    }

    private val scoreOld by lazy<Method> {
        DexKit.findMember("LockOneHundredPoints2") {
            it.findClass {
                matcher {
                    className = "com.miui.securityscan.scanner.ScoreManager"
                }
            }.findMethod {
                matcher {
                    usingNumbers(41, 100)
                    returnType = "int"
                }
            }.single()
        }
    }

    private val score3 by lazy<Method> {
        DexKit.findMember("LockOneHundredPointsField") {
            it.findClass {
                matcher {
                    className = "com.miui.securityscan.scanner.ScoreManager"
                }
            }.findMethod {
                matcher {
                    addUsingField {
                        type = "java.util.Map"
                    }
                    addInvoke {
                        addUsingField {
                            type = "int"
                        }
                        returnType = "int"
                    }
                }
            }.single()
        }
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").methodFinder()
            .filterByName("onClick")
            .filterByParamTypes(View::class.java)
            .first().createHook {
                returnConstant(null)
            }

        runCatching {
            logD(TAG, lpparam.packageName, "LockOneHundredPoints method is $score")
            logD(TAG, lpparam.packageName, "LockOneHundredPoints old method is $scoreOld")
            logD(TAG, lpparam.packageName, "LockOneHundredPoints 3 method is $score3")
            scoreOld.createHook {
                returnConstant(0)
            }
            score.createHook {
                returnConstant(0)
            }
            score3.createHook {
                returnConstant(true)
            }
        }.onFailure {
            logE(TAG, lpparam.packageName, "LockOneHundredPoints hook Failed: $it")
        }
    }
}
