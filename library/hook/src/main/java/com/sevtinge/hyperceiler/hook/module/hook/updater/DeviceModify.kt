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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.updater

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import java.lang.reflect.*


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
        DexKit.findMemberList<Method>("DeviceModify") {
            it.findMethod {
                matcher {
                    usingEqStrings("android.os.SystemProperties", "get", "get e")
                }
            }
        }.forEach { method ->
            method.hookBeforeMethod {
                if (it.args[0] == "ro.product.mod_device") it.result = deviceName
            }
            logI(TAG, this.lpparam.packageName, "dexkit method is $method")
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
