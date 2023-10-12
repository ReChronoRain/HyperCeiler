package com.sevtinge.cemiuiler.module.hook.browser

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge

object DebugMode : BaseHook() {
    private var found = false

    override fun init() {
        /* val result: List<DexMethodDescriptor> =
             Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode"))
         for (descriptor in result) {
             val DebugMode: Method = descriptor.getMethodInstance(lpparam.classLoader)
             if (DebugMode.returnType == Boolean::class.javaPrimitiveType && DebugMode.toString()
                     .contains("getDebugMode")
             ) {
                 log("DebugMode method is $DebugMode")
                 found = true
                 XposedBridge.hookMethod(DebugMode, XC_MethodReplacement.returnConstant(true))
             }
         }
         if (!found) {
             val result1: List<DexMethodDescriptor> =
                 Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode1"))
             for (descriptor1 in result1) {
                 val DebugMode1: Method = descriptor1.getMethodInstance(lpparam.classLoader)
                 if (DebugMode1.returnType == Boolean::class.javaPrimitiveType && DebugMode1.toString()
                         .contains("getDebugMode")
                 ) {
                     log("DebugMode1 method is $DebugMode1")
                     found = true
                     XposedBridge.hookMethod(
                         DebugMode1,
                         XC_MethodReplacement.returnConstant(true)
                     )
                 }
             }
         }
         if (!found) {
             val result2: List<DexMethodDescriptor> =
                 Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode2"))
             for (descriptor2 in result2) {
                 val DebugMode2: Method = descriptor2.getMethodInstance(lpparam.classLoader)
                 if (DebugMode2.returnType == Boolean::class.javaPrimitiveType && DebugMode2.toString()
                         .contains("getDebugMode")
                 ) {
                     log("DebugMode2 method is $DebugMode2")
                     XposedBridge.hookMethod(
                         DebugMode2,
                         XC_MethodReplacement.returnConstant(true)
                     )
                 }
             }
         }*/
        initDexKit(lpparam)
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("pref_key_debug_mode_new")
                returnType = "boolean"
            }
        }.forEach {
            val debugMode = it.getMethodInstance(lpparam.classLoader)
            if (debugMode.toString().contains("getDebugMode")) {
                logI("DebugMode method is $debugMode")
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
                    logI("DebugMode1 method is $debugMode1")
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
                    logI("DebugMode2 method is $debugMode2")
                    found = true
                    XposedBridge.hookMethod(
                        debugMode2,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        }
        closeDexKit()
    }
}
