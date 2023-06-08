package com.sevtinge.cemiuiler.module.updater

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.updater.UpdaterDexKit.mUpdaterResultMethodsMap
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import de.robv.android.xposed.XposedBridge
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
            XposedBridge.log("Cemiuiler: DeviceModify (Updater) android.os.SystemProperties hook failed by $e")
        }
        try {
            "miuix.core.util.SystemProperties".hookBeforeMethod(
                "get", String::class.java, String::class.java
            ) {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: DeviceModify (Updater) miuix.core.util.SystemProperties hook failed by $e")
        }
        try {
            val systemProperties = mUpdaterResultMethodsMap["SystemProperties"]!!
            assert(systemProperties.size == 1)
            val systemPropertiesDescriptor = systemProperties.first()
            val systemPropertiesMethod: Method =
                systemPropertiesDescriptor.getMethodInstance(lpparam.classLoader)
            systemPropertiesMethod.hookBeforeMethod {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
            log("(Updater) dexkit method is $systemPropertiesMethod")
        } catch (e: Throwable) {
            XposedBridge.log("Cemiuiler: DeviceModify (Updater) dexkit hook failed by $e")
        }
    }
}
