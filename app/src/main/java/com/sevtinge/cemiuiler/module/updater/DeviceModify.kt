package com.sevtinge.cemiuiler.module.updater

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import de.robv.android.xposed.XposedBridge
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

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
            log("(Updater) android.os.SystemProperties hook failed by $e")
        }
        try {
            "miuix.core.util.SystemProperties".hookBeforeMethod(
                "get", String::class.java, String::class.java
            ) {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
        } catch (e: Throwable) {
            log("(Updater) miuix.core.util.SystemProperties hook failed by $e")
        }
        try {
            System.loadLibrary("dexkit")
            DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
                val map = mapOf(
                    "SystemProperties" to setOf("android.os.SystemProperties", "get", "get e"),
                )
                val resultMap = bridge.batchFindMethodsUsingStrings {
                    queryMap(map)
                }
                val systemProperties = resultMap["SystemProperties"]!!
                assert(systemProperties.size == 1)
                val systemPropertiesDescriptor = systemProperties.first()
                val systemPropertiesMethod: Method =
                    systemPropertiesDescriptor.getMethodInstance(lpparam.classLoader)
                log("(Updater) dexkit method is $systemPropertiesMethod")
                systemPropertiesMethod.hookBeforeMethod {
                    if (it.args[0] == "ro.product.mod_device") it.result = deviceName
                }
            }
        } catch (e: Throwable) {
            log("(Updater) dexkit hook failed by $e")
        }
    }
}