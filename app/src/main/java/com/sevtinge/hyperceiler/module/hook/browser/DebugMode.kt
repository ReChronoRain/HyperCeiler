package com.sevtinge.hyperceiler.module.hook.browser

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.closeDexKit
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.DexKit.initDexKit
import com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge

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
