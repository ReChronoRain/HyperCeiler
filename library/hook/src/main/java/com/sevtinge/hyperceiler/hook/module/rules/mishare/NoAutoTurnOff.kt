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
package com.sevtinge.hyperceiler.hook.module.rules.mishare

import android.content.Context
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object NoAutoTurnOff : BaseHook() {
    private val stopAdvertAllMethod by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff9") {
            it.findMethod {
                matcher {
                    usingStrings("stopAdvertAll timeout. try stop ")
                }
            }.single()
        }
    }

    private val toastMethod by lazy<List<Method>> {
        DexKit.findMemberList("NoAutoTurnOff4") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingStrings("EnablingState processMessage(0x%X)", "MiShareService")
                    }
                    returnType = "boolean"
                    paramTypes = listOf("android.os.Message")
                }
            }
        }
    }

    private val toastMethodNew by lazy<List<Method>> {
        DexKit.findMemberList("NoAutoTurnOff4N") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingStrings("MiShareStateMachine")
                    }
                    paramCount = 0
                }
            }
        }
    }

    private val showToastMethod by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff5") {
            it.findMethod {
                matcher {
                    declaredClass {
                        fieldCount(0)
                        methodCount(2)
                    }
                    returnType = "void"
                    paramTypes = listOf("android.content.Context", "java.lang.CharSequence", "int")
                    modifiers = Modifier.STATIC
                }
            }.singleOrNull()
        }
    }

    override fun init() {

        // 禁用小米互传功能自动关闭部分
        stopAdvertAllMethod.createHook {
            returnConstant(null)
        }


        // 干掉小米互传十分钟倒计时 Toast
        if (toastMethod.isNotEmpty()) {
            toastMethod.createHooks {
                before { param ->
                    Context::class.java.methodFinder()
                        .filterByName("getString")
                        .filterByParamTypes {
                            it.size == 1 && it[0] == Int::class.java
                        }.first().createHook {
                            before { param ->
                                val resName =
                                    (param.thisObject as Context).resources.getResourceName(param.args[0] as Int)
                                if (resName == "com.miui.mishare.connectivity:string/toast_auto_close_in_minutes") param.result =
                                    "Modify by HyperCeiler"
                            }
                        }
                }
            }

            showToastMethod.createHook {
                before { param ->
                    if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                        null
                }
            }
        } else {
            toastMethodNew.createHooks {
                before { param ->
                    Context::class.java.methodFinder()
                        .filterByName("getString")
                        .filterByParamTypes {
                            it.size == 1 && it[0] == Int::class.java
                        }.first().createHook {
                            before { param ->
                                val resName =
                                    (param.thisObject as Context).resources.getResourceName(param.args[0] as Int)
                                if (resName == "com.miui.mishare.connectivity:string/toast_or_desc_advert_all_open") param.result =
                                    "Modify by HyperCeiler"
                            }
                        }
                }
            }
            showToastMethod.createHook {
                before { param ->
                    if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                        null
                }
            }
            setResReplacement("com.miui.mishare.connectivity", "string", "switch_mode_all", R.string.mishare_hook_switch_mode_all)
        }
    }
}
