package com.sevtinge.cemiuiler.module.hook.screenrecorder

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.isFinal

object ScreenRecorderConfig : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("Error when set frame value, maxValue = ")
            }
        }.forEach { methodData ->
            methodData.getMethodInstance(lpparam.classLoader).createHook {
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
                                        (it as IntArray).contentEquals(intArrayOf(15, 24, 30, 48, 60, 90))
                                    }.getOrDefault(false)
                                }
                        }
                    }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
                }
            }
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("defaultBitRate = ")
            }
        }.forEach { methodData ->
            methodData.getMethodInstance(lpparam.classLoader).createHook {
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
                                        (it as IntArray).contentEquals(intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
                                    }.getOrDefault(false)
                                }
                        }
                    }?.set(null, intArrayOf(1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
                }
            }
        }

       /* val mScreenRecorderConfigA = mScreenRecorderResultMethodsMap["ScreenRecorderConfigA"]!!
        assert(mScreenRecorderConfigA.size == 1)
        val mScreenRecorderConfigADescriptor = mScreenRecorderConfigA.first()
        val mScreenRecorderConfigAMethod: Method =
            mScreenRecorderConfigADescriptor.getMethodInstance(lpparam.classLoader)
        // XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
        mScreenRecorderConfigAMethod.createHook {
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
                                    (it as IntArray).contentEquals(intArrayOf(15, 24, 30, 48, 60, 90))
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
            }
        }


        val mScreenRecorderConfigB = mScreenRecorderResultMethodsMap["ScreenRecorderConfigB"]!!
        assert(mScreenRecorderConfigB.size == 1)
        val mScreenRecorderConfigBDescriptor = mScreenRecorderConfigB.first()
        val mScreenRecorderConfigBMethod: Method =
            mScreenRecorderConfigBDescriptor.getMethodInstance(lpparam.classLoader)
        // XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
        mScreenRecorderConfigBMethod.createHook {
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
                                    (it as IntArray).contentEquals(intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
                                }.getOrDefault(false)
                            }
                    }
                }?.set(null, intArrayOf(1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
            }
        }*/
    }
}
