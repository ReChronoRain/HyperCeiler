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
package com.sevtinge.hyperceiler.module.hook.mishare

import android.content.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toElementList
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toField
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import com.sevtinge.hyperceiler.module.base.tool.*
import com.sevtinge.hyperceiler.module.base.tool.HookTool.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.log.*
import de.robv.android.xposed.*
import java.lang.reflect.*

object NoAutoTurnOff : BaseHook() {
    private val nullMethod by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff1") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("MiShareService", "EnabledState")
                    usingNumbers(600000L)
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val nullMethodNew by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff1N") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("UnionShare", "EnabledState")
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val null2Method by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff2") {
            it.findMethod {
                matcher {
                    declaredClass {
                        addUsingStringsEquals("mishare:advertise_lock")
                    }
                    paramCount = 2
                    modifiers = Modifier.STATIC
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val null3Method by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff3") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("com.miui.mishare.action.GRANT_NFC_TOUCH_PERMISSION")
                    usingNumbers(600000L)
                    modifiers = Modifier.PRIVATE
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val toastMethod by lazy {
        DexKit.getDexKitBridgeList("NoAutoTurnOff4") {
            it.findMethod {
                matcher {
                    declaredClass {
                        addUsingStringsEquals("EnablingState processMessage(0x%X)", "MiShareService")
                    }
                    returnType = "boolean"
                    paramTypes = listOf("android.os.Message")
                }
            }.toElementList()
        }.toMethodList()
    }

    private val toastMethodNew by lazy {
        DexKit.getDexKitBridgeList("NoAutoTurnOff4N") {
            it.findMethod {
                matcher {
                    declaredClass {
                        addUsingStringsEquals("MiShareStateMachine")
                    }
                    paramCount = 0
                }
            }.toElementList()
        }.toMethodList()
    }

    private val showToastMethod by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff5") {
            it.findMethod {
                matcher {
                    declaredClass {
                        fieldCount(0)
                        methodCount(2)
                    }
                    returnType = "void"
                    modifiers = Modifier.STATIC
                    paramTypes = listOf("android.content.Context", "java.lang.CharSequence", "int")
                }
            }.singleOrNull()?.getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    private val nullField by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff6") {
            it.findField {
                matcher {
                    addReadMethod {
                        addUsingStringsEquals("NfcShareTaskManager")
                        returnType = "void"
                        paramCount = 1
                        modifiers = Modifier.PRIVATE
                    }
                    modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                    type = "int"
                }
            }.singleOrNull()?.getFieldInstance(safeClassLoader)
        }.toField()
    }

    private val null2Field by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff7") {
            it.findField {
                matcher {
                    addReadMethod {
                        addUsingStringsEquals("stopAdvertAllDelay")
                        returnType = "void"
                        paramCount = 0
                        modifiers = Modifier.PRIVATE
                    }
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                    type = "int"
                }
            }.singleOrNull()?.getFieldInstance(safeClassLoader)
        }.toField()
    }

    private val null2FieldMethod by lazy {
        DexKit.getDexKitBridge("NoAutoTurnOff8") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("stopAdvertAllDelay")
                    returnType = "void"
                    paramCount = 0
                    modifiers = Modifier.PRIVATE
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    override fun init() {
        // 禁用小米互传功能自动关闭部分
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
                } })
        }

        try {
            runCatching {
                hookMethod(null2FieldMethod, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        XposedHelpers.setObjectField(param.thisObject, null2Field.name, 2147483647)
                    } })
            }
        } catch (_: Exception) {
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
                    hookMethod(showToastMethod, object : MethodHook() {
                        @Throws(Throwable::class)
                        override fun before(param: MethodHookParam) {
                            if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                                null
                        }
                    })
                }
            }
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
                    hookMethod(showToastMethod, object : MethodHook() {
                        @Throws(Throwable::class)
                        override fun before(param: MethodHookParam) {
                            if (param.args[1].toString() == "Modify by HyperCeiler") param.result =
                                null
                        }
                    })
                }
            }
            mResHook.setResReplacement("com.miui.mishare.connectivity", "string", "switch_mode_all", R.string.mishare_hook_switch_mode_all)
        }

    }
}
