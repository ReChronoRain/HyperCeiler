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
package com.sevtinge.hyperceiler.hook.module.hook.home

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.api.LazyClass.SystemProperties
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

object SetDeviceLevel : BaseHook() {
    private val mDeviceLevelUtilsClass by lazy {
        loadClass("com.miui.home.launcher.common.DeviceLevelUtils")
    }
    private val mDeviceConfigClass by lazy {
        loadClass("com.miui.home.launcher.DeviceConfig")
    }

    override fun init() {
        runCatching {
            loadClass("com.miui.home.launcher.common.CpuLevelUtils").methodFinder()
                .filterByName("getQualcommCpuLevel")
                .filterByParamCount(1)
                .single()
        }.recoverCatching {
            loadClass("miuix.animation.utils.DeviceUtils").methodFinder()
                .filterByName("getQualcommCpuLevel")
                .filterByParamCount(1)
                .single()
        }.getOrElse {
            loadClass("miuix.device.DeviceUtils").methodFinder()
                .filterByName("getQualcommCpuLevel")
                .filterByParamCount(1)
                .single()
        }.createHook {
            returnConstant(2)
        }

        runCatching {
            mDeviceConfigClass.methodFinder()
                .filterByName("isUseSimpleAnim")
                .single().createHook {
                    returnConstant(false)
            }
        }
        runCatching {
            mDeviceLevelUtilsClass.methodFinder()
                .filterByName("getDeviceLevel")
                .single().createHook {
                    returnConstant(2)
            }
        }
        runCatching {
            mDeviceConfigClass.methodFinder()
                .filterByName("isSupportCompleteAnimation")
                .single().createHook {
                    returnConstant(true)
            }
        }
        runCatching {
            mDeviceLevelUtilsClass.methodFinder()
                .filterByName("isLowLevelOrLiteDevice")
                .single().createHook {
                    returnConstant(false)
            }
        }
        runCatching {
            mDeviceConfigClass.methodFinder()
                .filterByName("isMiuiLiteVersion")
                .single().createHook {
                    returnConstant(false)
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
