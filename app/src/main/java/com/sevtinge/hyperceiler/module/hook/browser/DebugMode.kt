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
package com.sevtinge.hyperceiler.module.hook.browser

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.*
import de.robv.android.xposed.*

object DebugMode : BaseHook() {
    private var found = false

    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("environment_flag")
                returnType = "java.lang.String"
            }
        }.forEach {
            val environmentFlag = it.getMethodInstance(lpparam.classLoader)
            logI(TAG, this.lpparam.packageName, "environmentFlag method is $environmentFlag")
            XposedBridge.hookMethod(
                environmentFlag,
                XC_MethodReplacement.returnConstant("1")
            )
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("pref_key_debug_mode_new")
                returnType = "boolean"
            }
        }.forEach {
            val debugMode = it.getMethodInstance(lpparam.classLoader)
            if (debugMode.toString().contains("getDebugMode")) {
                logI(TAG, this.lpparam.packageName, "DebugMode method is $debugMode")
                found = true
                XposedBridge.hookMethod(
                    debugMode,
                    XC_MethodReplacement.returnConstant(true)
                )
            }
        }

        if (!found) {
            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals("pref_key_debug_mode")
                    returnType = "boolean"
                }
            }.forEach {
                val debugMode1 = it.getMethodInstance(safeClassLoader)
                if (debugMode1.toString().contains("getDebugMode")) {
                    logI(TAG, this.lpparam.packageName, "DebugMode1 method is $debugMode1")
                    found = true
                    XposedBridge.hookMethod(
                        debugMode1,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        }

        if (!found) {
            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals("pref_key_debug_mode_" + getPackageVersionCode(lpparam))
                    returnType = "boolean"
                }
            }.forEach {
                val debugMode2 = it.getMethodInstance(lpparam.classLoader)
                if (debugMode2.toString().contains("getDebugMode")) {
                    logI(TAG, this.lpparam.packageName, "DebugMode2 method is $debugMode2")
                    found = true
                    XposedBridge.hookMethod(
                        debugMode2,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        }
    }
}
