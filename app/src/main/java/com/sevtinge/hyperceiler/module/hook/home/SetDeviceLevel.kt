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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.LazyClass.SystemProperties
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookBeforeMethod
import com.sevtinge.hyperceiler.utils.replaceMethod

object SetDeviceLevel : BaseHook() {
    override fun init() {
        val mDeviceLevelUtilsClass = loadClass("com.miui.home.launcher.common.DeviceLevelUtils")
        val mDeviceConfigClass = loadClass("com.miui.home.launcher.DeviceConfig")

        try {
            loadClass("com.miui.home.launcher.common.CpuLevelUtils").methodFinder().first {
                name == "getQualcommCpuLevel" && parameterCount == 1
            }
        } catch (e: Exception) {
            loadClass("miuix.animation.utils.DeviceUtils").methodFinder().first {
                name == "getQualcommCpuLevel" && parameterCount == 1
            }
        }.createHook { returnConstant(2) }

        runCatching {
            mDeviceConfigClass.methodFinder().first {
                name == "isUseSimpleAnim"
            }.createHook {
                before { it.result = false }
            }
        }
        runCatching {
            mDeviceLevelUtilsClass.methodFinder().first {
                name == "getDeviceLevel"
            }.createHook {
                before { it.result = 2 }
            }
        }
        runCatching {
            mDeviceConfigClass.methodFinder().first {
                name == "isSupportCompleteAnimation"
            }.createHook {
                before { it.result = true }
            }
        }
        runCatching {
            mDeviceLevelUtilsClass.methodFinder().first {
                name == "isLowLevelOrLiteDevice"
            }.createHook {
                before { it.result = false }
            }
        }
        runCatching {
            mDeviceConfigClass.methodFinder().first {
                name == "isMiuiLiteVersion"
            }.createHook {
                before { it.result = false }
            }
        }
        runCatching {
            "com.miui.home.launcher.util.noword.NoWordSettingHelperKt".hookBeforeMethod(
                "isNoWordAvailable"
            ) { it.result = true }
        }

        runCatching {
            SystemProperties.methodFinder().filter {
                name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java
            }.toList().createHooks {
                before {
                    if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                    if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
                }
            }
        }
        runCatching {
            "com.miui.home.launcher.common.Utilities".hookBeforeMethod(
                "canLockTaskView"
            ) { it.result = true }
        }
        runCatching {
            "com.miui.home.launcher.MIUIWidgetUtil".hookBeforeMethod(
                "isMIUIWidgetSupport"
            ) {
                it.result = true
            }
        }
        runCatching {
            "com.miui.home.launcher.MiuiHomeLog".findClass().replaceMethod(
                "log", String::class.java, String::class.java
            ) {
                return@replaceMethod null
            }
        }
        runCatching {
            "com.xiaomi.onetrack.OneTrack".hookBeforeMethod("isDisable") {
                it.result = true
            }
        }
    }
}
