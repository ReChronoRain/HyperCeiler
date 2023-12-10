package com.sevtinge.hyperceiler.module.hook.screenrecorder

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.isFinal

object ScreenRecorderConfig : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("Error when set frame value, maxValue = ")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            before { param ->
                param.args[0] = 1200
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                            fieldAccessible.get(null).let {
                                runCatching {
                                    (it as IntArray).contentEquals(
                                        intArrayOf(15, 24, 30, 48, 60, 90)
                                    )
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
            }
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("defaultBitRate = ")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            before { param ->
                param.args[0] = 1200
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal &&
                            fieldAccessible.get(null).let {
                                runCatching {
                                    (it as IntArray).contentEquals(
                                        intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1)
                                    )
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
            }
        }
    }
}
