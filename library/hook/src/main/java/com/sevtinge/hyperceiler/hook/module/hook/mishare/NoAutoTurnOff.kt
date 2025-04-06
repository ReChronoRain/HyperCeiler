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
package com.sevtinge.hyperceiler.hook.module.hook.mishare

import android.content.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import de.robv.android.xposed.*
import java.lang.reflect.*

object NoAutoTurnOff : BaseHook() {
    private val nullMethod by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff1") {
            it.findMethod {
                matcher {
                    usingStrings("MiShareService", "EnabledState")
                    usingNumbers(600000L)
                }
            }.single()
        }
    }

    private val nullMethodNew by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff1N") {
            it.findMethod {
                matcher {
                    usingStrings("UnionShare", "EnabledState")
                }
            }.single()
        }
    }

    private val null2Method by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff2") {
            it.findMethod {
                matcher {
                    declaredClass {
                        usingStrings("mishare:advertise_lock")
                    }
                    paramCount = 2
                    modifiers = Modifier.STATIC
                }
            }.single()
        }
    }

    private val null3Method by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff3") {
            it.findMethod {
                matcher {
                    usingStrings("com.miui.mishare.action.GRANT_NFC_TOUCH_PERMISSION")
                    usingNumbers(600000L)
                    modifiers = Modifier.PRIVATE
                }
            }.single()
        }
    }

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

    private val nullField by lazy<Field> {
        DexKit.findMember("NoAutoTurnOff6") {
            it.findField {
                matcher {
                    addReadMethod {
                        usingStrings("NfcShareTaskManager")
                        returnType = "void"
                        paramCount = 1
                        modifiers = Modifier.PRIVATE
                    }
                    modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                    type = "int"
                }
            }.singleOrNull()
        }
    }

    private val null2Field by lazy<Field> {
        DexKit.findMember("NoAutoTurnOff7") {
            it.findField {
                matcher {
                    addReadMethod {
                        usingStrings("stopAdvertAllDelay")
                        returnType = "void"
                        paramCount = 0
                        modifiers = Modifier.PRIVATE
                    }
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                    type = "int"
                }
            }.singleOrNull()
        }
    }

    private val null2FieldMethod by lazy<Method> {
        DexKit.findMember("NoAutoTurnOff8") {
            it.findMethod {
                matcher {
                    usingStrings("stopAdvertAllDelay")
                    returnType = "void"
                    paramCount = 0
                    modifiers = Modifier.PRIVATE
                }
            }.single()
        }
    }

    override fun init() {

        // 禁用小米互传功能自动关闭部分
        if (isMoreHyperOSVersion(2f)) {
            hookMethod(stopAdvertAllMethod, object : MethodHook() {
                override fun before(param: MethodHookParam?) {
                    param!!.result = null
                }
            })
        } else {
            runCatching {
                try {
                    setOf(nullMethod, null2Method).createHooks {
                        returnConstant(null)
                    }
                } catch (_: Exception) {
                    setOf(nullMethodNew, null2Method).createHooks {
                        returnConstant(null)
                    }
                }
            }

            runCatching {
                null3Method.createHook {
                    after {
                        val d = it.thisObject.getObjectField("d")
                        XposedHelpers.callMethod(d, "removeCallbacks", it.thisObject)
                        logI(
                            TAG, this@NoAutoTurnOff.lpparam.packageName,
                            "null3Method hook success, $d"
                        )
                    }
                }
            }

            runCatching {
                findAndHookConstructor(nullField.javaClass, object : MethodHook() {
                    override fun after(param: MethodHookParam) {
                        XposedHelpers.setObjectField(param.thisObject, nullField.name, 2147483647)
                        logI(TAG, lpparam.packageName, "nullField hook success, $nullField")
                    }
                })
            }

            try {
                runCatching {
                    hookMethod(null2FieldMethod, object : MethodHook() {
                        override fun before(param: MethodHookParam) {
                            XposedHelpers.setObjectField(
                                param.thisObject,
                                null2Field.name,
                                2147483647
                            )
                        }
                    })
                }
            } catch (_: Exception) {
            }
        }

        // 干掉小米互传十分钟倒计时 Toast
        if (toastMethod.isNotEmpty()) {
            toastMethod.createHooks {
                before { param ->
                    findAndHookMethod(
                        Context::class.java,
                        "getString",
                        Int::class.java,
                        object : MethodHook() {
                            override fun before(param: MethodHookParam) {
                                val resName =
                                    (param.thisObject as Context).resources.getResourceName(param.args[0] as Int)
                                if (resName == "com.miui.mishare.connectivity:string/toast_auto_close_in_minutes") param.result =
                                    "Modify by HyperCeiler"
                            }
                        })
                }
            }
            hookMethod(showToastMethod, object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                        null
                }
            })
        } else {
            toastMethodNew.createHooks {
                before { param ->
                    findAndHookMethod(
                        Context::class.java,
                        "getString",
                        Int::class.java,
                        object : MethodHook() {
                            override fun before(param: MethodHookParam) {
                                val resName =
                                    (param.thisObject as Context).resources.getResourceName(param.args[0] as Int)
                                if (resName == "com.miui.mishare.connectivity:string/toast_or_desc_advert_all_open") param.result =
                                    "Modify by HyperCeiler"
                            }
                        })
                }
            }
            hookMethod(showToastMethod, object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                        null
                }
            })
            mResHook.setResReplacement("com.miui.mishare.connectivity", "string", "switch_mode_all", R.string.mishare_hook_switch_mode_all)
        }
    }
}
