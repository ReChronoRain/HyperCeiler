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

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.getObjectField
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

@SuppressLint("StaticFieldLeak")
object NoAutoTurnOff : BaseHook() {
    private val nullMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("MiShareService", "EnabledState")
                usingNumbers(600000L)
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    private val null2Method by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("mishare:advertise_lock")
                }
                paramCount = 2
                modifiers = Modifier.STATIC
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    private val null3Method by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("com.miui.mishare.action.GRANT_NFC_TOUCH_PERMISSION")
                usingNumbers(600000L)
                modifiers = Modifier.PRIVATE
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    private val toastMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("null context", "cta_agree")
                }
                returnType = "boolean"
                paramTypes = listOf("android.content.Context", "java.lang.String")
                paramCount = 2
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    override fun init() {
        val nullClass = dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("NfcShareTaskManager", "task out of limit type ")
            }
        }.map { it.getInstance(classLoader) }.first()

        val nullField = dexKitBridge.findField {
            matcher {
                addReadMethod {
                    addUsingStringsEquals("NfcShareTaskManager")
                    returnType = "void"
                    paramCount = 1
                    modifiers = Modifier.PRIVATE
                }
                modifiers = Modifier.STATIC or Modifier.FINAL
                type = "int"
            }
        }.singleOrNull()?.getFieldInstance(safeClassLoader)

        // 禁用小米互传功能自动关闭部分
        try {
        nullMethod.createHook {
            returnConstant(null)
        }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "nullMethod hook failed", t)
        }

        try {
            null2Method.createHook {
                returnConstant(null)
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "null2Method hook failed", t)
        }

        try {
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
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "null3Method hook failed", t)
        }

        try {
            findAndHookConstructor(nullClass, object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    XposedHelpers.setObjectField(param.thisObject, nullField!!.name, 999999999)
                    logI(nullField.name)
                } })
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, "nullField hook failed", t)
        }


        // 干掉小米互传十分钟倒计时 Toast
        toastMethod.createHooks {
            before { param ->
                if (param.args[1].equals("security_agree")) {
                    param.result = false
                }
            }
        }
    }
}
