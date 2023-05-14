package com.sevtinge.cemiuiler.module.screenrecorder

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.isFinal
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.screenrecorder.ScreenRecorderDexKit.mScreenRecorderResultMethodsMap
import java.lang.reflect.Method

object ScreenRecorderConfig : BaseHook() {
    override fun init() {
        val mScreenRecorderConfigA = mScreenRecorderResultMethodsMap["ScreenRecorderConfigA"]!!
        assert(mScreenRecorderConfigA.size == 1)
        val mScreenRecorderConfigADescriptor = mScreenRecorderConfigA.first()
        val mScreenRecorderConfigAMethod: Method =
            mScreenRecorderConfigADescriptor.getMethodInstance(lpparam.classLoader)
        //XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
        mScreenRecorderConfigAMethod.hookBefore { param ->
            param.args[0] = 1200
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


        val mScreenRecorderConfigB = mScreenRecorderResultMethodsMap["ScreenRecorderConfigB"]!!
        assert(mScreenRecorderConfigB.size == 1)
        val mScreenRecorderConfigBDescriptor = mScreenRecorderConfigB.first()
        val mScreenRecorderConfigBMethod: Method =
            mScreenRecorderConfigBDescriptor.getMethodInstance(lpparam.classLoader)
        //XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit method is $systemPropertiesMethod")
        mScreenRecorderConfigBMethod.hookBefore { param ->
            param.args[0] = 1200
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
            }?.set(null, intArrayOf(1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1))
        }
    }

}