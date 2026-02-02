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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.SystemProperties
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

object SetDeviceLevel : HomeBaseHookNew() {
    private val mDeviceLevelUtilsClass by lazy {
        loadClass("com.miui.home.launcher.common.DeviceLevelUtils")
    }
    private val mDeviceLevelUtilsNewClass by lazy {
        loadClass("com.miui.home.common.utils.DeviceLevelUtils")
    }
    private val mDeviceConfigClass by lazy {
        loadClass("com.miui.home.launcher.DeviceConfig")
    }
    private val mDeviceConfigNewClass by lazy {
        loadClass("com.miui.home.common.device.DeviceConfigs")
    }

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        loadClass("miuix.device.DeviceUtils").methodFinder()
            .filterByName("getQualcommCpuLevel")
            .single().createHook {
                returnConstant(2)
            }

        mDeviceConfigClass.methodFinder()
            .filterByName("isSupportCompleteAnimation")
            .single().createHook {
                returnConstant(true)
            }

        mDeviceConfigNewClass.methodFinder().apply {
            filterByName("isLowLevelDevice")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isMiuiLiteVersion")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isMiuiLiteOrMiddleVersion")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isSupportSuperXiaoai")
                .single().createHook {
                    returnConstant(true)
                }
        }

        mDeviceLevelUtilsNewClass.methodFinder().apply {
            filterByName("isUseSimpleAnim")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("getDeviceLevel")
                .single().createHook {
                    returnConstant(2)
                }

            filterByName("isLowLevelOrLiteDevice")
                .single().createHook {
                    returnConstant(false)
                }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").beforeHookMethod(
            "isNoWordAvailable"
        ) { it.result = true }

        SystemProperties.methodFinder().filter {
            name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java
        }.toList().createHooks {
            before {
                if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
            }
        }

        findClass("com.miui.home.launcher.common.Utilities").beforeHookMethod(
            "canLockTaskView"
        ) { it.result = true }

        findClass("com.miui.home.common.MiuiHomeLog").replaceMethod(
            "log", String::class.java, String::class.java
        ) {
            return@replaceMethod null
        }

        findClass("com.xiaomi.onetrack.OneTrack").beforeHookMethod("isDisable") {
            it.result = true
        }
    }

    @Version(isPad = true, min = 450000000)
    private fun initPadHook() {
        loadClass("com.miui.home.launcher.common.CpuLevelUtils").methodFinder()
            .filterByName("getQualcommCpuLevel")
            .filterByParamCount(1)
            .single().createHook {
                returnConstant(2)
            }

        loadClass("miuix.device.DeviceUtils").methodFinder()
            .filterByName("getQualcommCpuLevel")
            .single().createHook {
                returnConstant(2)
            }

        mDeviceConfigClass.methodFinder().apply {
            filterByName("isMiuiLiteVersion")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isSupportCompleteAnimation")
                .single().createHook {
                    returnConstant(true)
                }

            filterByName("isMiuiLiteOrMiddleVersion")
                .single().createHook {
                    returnConstant(false)
                }
        }

        mDeviceLevelUtilsClass.methodFinder().apply {
            filterByName("isLowLevelDevice")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isLowLevelDeviceFromFolme")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isUseSimpleAnim")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("getDeviceLevel")
                .single().createHook {
                    returnConstant(2)
                }

            filterByName("isLowLevelOrLiteDevice")
                .single().createHook {
                    returnConstant(false)
                }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").beforeHookMethod(
            "isNoWordAvailable"
        ) { it.result = true }

        SystemProperties.methodFinder().filter {
            name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java
        }.toList().createHooks {
            before {
                if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
            }
        }

        findClass("com.miui.home.launcher.common.Utilities").beforeHookMethod(
            "canLockTaskView"
        ) { it.result = true }

        findClass("com.miui.home.launcher.MiuiHomeLog").replaceMethod(
            "log", String::class.java, String::class.java
        ) {
            return@replaceMethod null
        }

        findClass("com.xiaomi.onetrack.OneTrack").beforeHookMethod("isDisable") {
            it.result = true
        }
    }

    override fun initBase() {
        if (isPad()) {
            initPadHook()
            return
        }

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

        mDeviceConfigClass.methodFinder().apply {
            filterByName("isUseSimpleAnim")
                .single().createHook {
                    returnConstant(false)
                }

            filterByName("isSupportCompleteAnimation")
                .single().createHook {
                    returnConstant(true)
                }

            filterByName("isMiuiLiteVersion")
                .single().createHook {
                    returnConstant(false)
                }
        }

        mDeviceLevelUtilsClass.methodFinder().apply {
            filterByName("getDeviceLevel")
                .single().createHook {
                    returnConstant(2)
                }

            filterByName("isLowLevelOrLiteDevice")
                .single().createHook {
                    returnConstant(false)
                }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").beforeHookMethod(
            "isNoWordAvailable"
        ) { it.result = true }

        SystemProperties.methodFinder().filter {
            name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java
        }.toList().createHooks {
            before {
                if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
            }
        }

        findClass("com.miui.home.launcher.common.Utilities").beforeHookMethod(
            "canLockTaskView"
        ) { it.result = true }

        findClass("com.miui.home.launcher.MIUIWidgetUtil").beforeHookMethod(
            "isMIUIWidgetSupport"
        ) {
            it.result = true
        }

        findClass("com.miui.home.launcher.MiuiHomeLog").replaceMethod(
            "log", String::class.java, String::class.java
        ) {
            return@replaceMethod null
        }

        findClass("com.xiaomi.onetrack.OneTrack").beforeHookMethod("isDisable") {
            it.result = true
        }
    }
}
