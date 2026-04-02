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
package com.sevtinge.hyperceiler.libhook.rules.mishare

import android.content.Context
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.chainMethod
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object NoAutoTurnOff : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        stopAdvertAllMethod
        showToastMethod
        toastMethodNew
        return true
    }
    private val stopAdvertAllMethod by lazy {
        optionalMember("NoAutoTurnOff9") {
            it.findMethod {
                matcher {
                    usingStrings("stopAdvertAll timeout. try stop ")
                }
            }.single()
        } as? Method
    }

    private val toastMethodNew by lazy<List<Method>> {
        optionalMemberList("NoAutoTurnOff4N") {
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

    private val showToastMethod by lazy<Method?> {
        optionalMember("NoAutoTurnOff5") {
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
        stopAdvertAllMethod?.createHook {
            returnConstant(null)
        }


        // 干掉小米互传十分钟倒计时 Toast
        Context::class.java.chainMethod("getString", Int::class.javaPrimitiveType!!) {
            val resName = runCatching {
                (thisObject as Context).resources.getResourceName(getArg(0) as Int)
            }.getOrNull()
            if (
                resName == "com.miui.mishare.connectivity:string/toast_auto_close_in_minutes" ||
                resName == "com.miui.mishare.connectivity:string/toast_or_desc_advert_all_open"
            ) {
                return@chainMethod "Modify by HyperCeiler"
            }
            proceed()
        }

        showToastMethod?.createHook {
            before { param ->
                if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                    null
            }
        }

        if (toastMethodNew.isNotEmpty()) {
            setResReplacement("com.miui.mishare.connectivity", "string", "switch_mode_all", R.string.mishare_hook_switch_mode_all)
        }
    }
}
