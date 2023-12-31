package com.sevtinge.hyperceiler.module.hook.updater

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.hookBeforeMethod


object DeviceModify : BaseHook() {
    private val deviceName: String = mPrefsMap.getString("updater_device", "")
    override fun init() {
        try {
            "android.os.SystemProperties".hookBeforeMethod(
                "get", String::class.java, String::class.java
            ) {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
        } catch (e: Throwable) {
            logE(TAG, "[DeviceModify(Updater)]: android.os.SystemProperties hook failed", e)
        }
        try {
            "miuix.core.util.SystemProperties".hookBeforeMethod(
                "get", String::class.java, String::class.java
            ) {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
        } catch (e: Throwable) {
            logE(
                TAG,
                "[DeviceModify(Updater)]: DeviceModify (Updater) miuix.core.util.SystemProperties hook failed",
                e
            )
        }
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.os.SystemProperties", "get", "get e")
            }
        }.forEach { methodData ->
            methodData.getMethodInstance(lpparam.classLoader).hookBeforeMethod {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
            logI(TAG, this.lpparam.packageName, "dexkit method is $methodData")
        }

        /*try {
            val systemProperties = mUpdaterResultMethodsMap["SystemProperties"]!!
            assert(systemProperties.size == 1)
            val systemPropertiesDescriptor = systemProperties.first()
            val systemPropertiesMethod: Method =
                systemPropertiesDescriptor.getMethodInstance(lpparam.classLoader)
            systemPropertiesMethod.hookBeforeMethod {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
            logI("(Updater) dexkit method is $systemPropertiesMethod")
        } catch (e: Throwable) {
            XposedBridge.log("[HyperCeiler][E][DeviceModify(Updater)]: DeviceModify (Updater) dexkit hook failed by $e")
        }*/
    }
}
