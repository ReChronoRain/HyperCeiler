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
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

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
        loadClass("miuix.device.DeviceUtils").findMethod { name("getQualcommCpuLevel") }
            .createHook {
                returnConstant(2)
            }

        mDeviceConfigClass.findMethod { name("isSupportCompleteAnimation") }.createHook {
            returnConstant(true)
        }

        mDeviceConfigNewClass.apply {
            findMethod {
                name("isLowLevelDevice")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isMiuiLiteVersion")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isMiuiLiteOrMiddleVersion")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isSupportSuperXiaoai")
            }.createHook {
                returnConstant(true)
            }
        }

        mDeviceLevelUtilsNewClass.apply {
            findMethod {
                name("isUseSimpleAnim")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("getDeviceLevel")
            }.createHook {
                returnConstant(2)
            }

            findMethod {
                name("isLowLevelOrLiteDevice")
            }.createHook {
                returnConstant(false)
            }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").findMethod {
            name("isNoWordAvailable")
        }.createBeforeHook {
            it.result = true
        }

        SystemProperties.findAllMethods { filter { name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java } }
            .createHooks {
                before {
                    if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                    if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
                }
            }

        findClass("com.miui.home.launcher.common.Utilities").findMethod {
            name("canLockTaskView")
        }.createBeforeHook {
            it.result = true
        }

        findClass("com.miui.home.common.MiuiHomeLog").findMethod {
            name("log"); parameterTypes(
            String::class.java,
            String::class.java
        )
        }.createHook {
            replace { null }
        }

        findClass("com.xiaomi.onetrack.OneTrack").findMethod {
            name("isDisable")
        }.createBeforeHook {
            it.result = true
        }
    }

    @Version(isPad = true, min = 450000000)
    private fun initPadHook() {
        loadClass("com.miui.home.launcher.common.CpuLevelUtils").findMethod {
            name("getQualcommCpuLevel"); paramCount(
            1
        )
        }.createHook {
            returnConstant(2)
        }

        loadClass("miuix.device.DeviceUtils").findMethod { name("getQualcommCpuLevel") }
            .createHook {
                returnConstant(2)
            }

        mDeviceConfigClass.apply {
            findMethod {
                name("isMiuiLiteVersion")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isSupportCompleteAnimation")
            }.createHook {
                returnConstant(true)
            }

            findMethod {
                name("isMiuiLiteOrMiddleVersion")
            }.createHook {
                returnConstant(false)
            }
        }

        mDeviceLevelUtilsClass.apply {
            findMethod {
                name("isLowLevelDevice")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isLowLevelDeviceFromFolme")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isUseSimpleAnim")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("getDeviceLevel")
            }.createHook {
                returnConstant(2)
            }

            findMethod {
                name("isLowLevelOrLiteDevice")
            }.createHook {
                returnConstant(false)
            }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").findMethod {
            name("isNoWordAvailable")
        }.createBeforeHook {
            it.result = true
        }

        SystemProperties.findAllMethods { filter { name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java } }
            .createHooks {
                before {
                    if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                    if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
                }
            }

        findClass("com.miui.home.launcher.common.Utilities").findMethod {
            name("canLockTaskView")
        }.createBeforeHook {
            it.result = true
        }

        findClass("com.miui.home.launcher.MiuiHomeLog").findMethod {
            name("log"); parameterTypes(
            String::class.java,
            String::class.java
        )
        }.createHook {
            replace { null }
        }

        findClass("com.xiaomi.onetrack.OneTrack").findMethod {
            name("isDisable")
        }.createBeforeHook {
            it.result = true
        }
    }

    override fun initBase() {
        if (isPad()) {
            initPadHook()
            return
        }

        runCatching {
            loadClass("com.miui.home.launcher.common.CpuLevelUtils").findMethod {
                name("getQualcommCpuLevel"); paramCount(
                1
            )
            }
        }.recoverCatching {
            loadClass("miuix.animation.utils.DeviceUtils").findMethod {
                name("getQualcommCpuLevel"); paramCount(
                1
            )
            }
        }.getOrElse {
            loadClass("miuix.device.DeviceUtils").findMethod {
                name("getQualcommCpuLevel"); paramCount(
                1
            )
            }
        }.createHook {
            returnConstant(2)
        }

        mDeviceConfigClass.apply {
            findMethod {
                name("isUseSimpleAnim")
            }.createHook {
                returnConstant(false)
            }

            findMethod {
                name("isSupportCompleteAnimation")
            }.createHook {
                returnConstant(true)
            }

            findMethod {
                name("isMiuiLiteVersion")
            }.createHook {
                returnConstant(false)
            }
        }

        mDeviceLevelUtilsClass.apply {
            findMethod {
                name("getDeviceLevel")
            }.createHook {
                returnConstant(2)
            }

            findMethod {
                name("isLowLevelOrLiteDevice")
            }.createHook {
                returnConstant(false)
            }
        }

        findClass("com.miui.home.launcher.util.noword.NoWordSettingHelperKt").findMethod {
            name("isNoWordAvailable")
        }.createBeforeHook {
            it.result = true
        }

        SystemProperties.findAllMethods { filter { name == "getBoolean" && parameterTypes[0] == String::class.java && parameterTypes[1] == Boolean::class.java } }
            .createHooks {
                before {
                    if (it.args[0] == "ro.config.low_ram.threshold_gb") it.result = false
                    if (it.args[0] == "ro.miui.backdrop_sampling_enabled") it.result = true
                }
            }

        findClass("com.miui.home.launcher.common.Utilities").findMethod {
            name("canLockTaskView")
        }.createBeforeHook {
            it.result = true
        }

        findClass("com.miui.home.launcher.MIUIWidgetUtil").findMethod {
            name("isMIUIWidgetSupport")
        }.createBeforeHook {
            it.result = true
        }

        findClass("com.miui.home.launcher.MiuiHomeLog").findMethod {
            name("log"); parameterTypes(
            String::class.java,
            String::class.java
        )
        }.createHook {
            replace { null }
        }

        findClass("com.xiaomi.onetrack.OneTrack").findMethod {
            name("isDisable")
        }.createBeforeHook {
            it.result = true
        }
    }
}
