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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other

import android.view.View
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object LockOneHundredPoints : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        scoreOld
        score
        score3
        addRiskApp
        return true
    }
    private val scoreOld by lazy<Method> {
        requiredMember("LockOneHundredPoints1") {
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

    private val score by lazy<Method> {
        requiredMember("LockOneHundredPoints2") {
            it.findClass {
                matcher {
                    className = "com.miui.securityscan.scanner.ScoreManager"
                }
            }.findMethod {
                matcher {
                    usingNumbers(41, 100, 0)
                    returnType = "int"
                }
            }.single()
        }
    }

    private val score3 by lazy<Method> {
        requiredMember("LockOneHundredPointsField") {
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

    private val addRiskApp by lazy<Method> {
        requiredMember("AddRiskApp") {
            it.findClass {
                matcher {
                    className = "com.miui.securityscan.scanner.ScoreManager"
                }
            }.findMethod {
                matcher {
                    addInvoke("Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z")
                    // 改动日志 (2026.11.06)：
                    // ScoreManager 里“1 个入参 + void 返回 + 调用 TextUtils#isEmpty”在 OS4 上有两个同构方法：
                    //   ScoreManager#V(Landroid/content/Context;)V  扫描应用列表（File#exists、List#add、Map#get）
                    //   ScoreManager#c(Ly4/g;)V                     取名字判空后 Map#put 登记风险应用（才是真正的 addRiskApp）
                    // 两版都是 2 个命中，.single() 抛 NonUniqueResultException，整条规则被跳过（日志：unique result: 2）。
                    // OS3.0.319 里只有一个候选（ScoreManager#c(Lcom/miui/antivirus/model/VirusModel;)V），因此不会出现同一错误。
                    // 两个候选的唯一结构差异在于是否向 Map 里 put（登记），“登记风险应用”这个动作本身就是本 Hook 的意图，
                    // 所以用 Map#put 调用去歧义。已在 OS3.0.319、OS4.0.0.44、OS4.0.5.0 三版离线复算：均收敛为 1 个命中。
                    addInvoke("Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                    paramCount = 1
                    returnType = "void"
                }
            }.single()
        }
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").findMethod { name("onClick"); parameterTypes(View::class.java) }.createHook {
                returnConstant(null)
            }

        runCatching {
            scoreOld.createHook {
                returnConstant(0)
            }
            score.createHook {
                returnConstant(100)
            }
            score3.createHook {
                returnConstant(true)
            }
            addRiskApp.createHook {
                returnConstant(null)
            }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "LockOneHundredPoints hook Failed: $it")
        }
    }
}

