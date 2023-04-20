package com.sevtinge.cemiuiler.module.screenrecorder

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.isFinal
import com.sevtinge.cemiuiler.module.base.BaseHook
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

object ScreenRecorderConfig : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
            val map = mapOf(
                "ScreenRecorderConfigA" to setOf("Error when set frame value, maxValue = "),
            )
            val resultMap = bridge.batchFindMethodsUsingStrings {
                queryMap(map)
            }
            val mScreenRecorderConfigA = resultMap["ScreenRecorderConfigA"]!!
            assert(mScreenRecorderConfigA.size == 1)
            val mScreenRecorderConfigADescriptor = mScreenRecorderConfigA.first()
            val mScreenRecorderConfigAMethod: Method =
                mScreenRecorderConfigADescriptor.getMethodInstance(lpparam.classLoader)
            //XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
            mScreenRecorderConfigAMethod.hookBefore { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                                fieldAccessible.get(null).let {
                                    kotlin.runCatching {
                                        (it as IntArray).contentEquals(intArrayOf(15, 24, 30, 48, 60, 90))
                                    }.getOrDefault(false)
                                }
                    }
                }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
            }
        }
        System.loadLibrary("dexkit")
        DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
            val map = mapOf(
                "ScreenRecorderConfigB" to setOf("defaultBitRate = "),
            )
            val resultMap = bridge.batchFindMethodsUsingStrings {
                queryMap(map)
            }
            val mScreenRecorderConfigB = resultMap["ScreenRecorderConfigB"]!!
            assert(mScreenRecorderConfigB.size == 1)
            val mScreenRecorderConfigBDescriptor = mScreenRecorderConfigB.first()
            val mScreenRecorderConfigBMethod: Method =
                mScreenRecorderConfigBDescriptor.getMethodInstance(lpparam.classLoader)
            //XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
            mScreenRecorderConfigBMethod.hookBefore { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                                fieldAccessible.get(null).let {
                                    kotlin.runCatching {
                                        (it as IntArray).contentEquals(intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
                                    }.getOrDefault(false)
                                }
                    }
                }?.set(null, intArrayOf(3600, 2400, 1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
            }
        }
    }
}