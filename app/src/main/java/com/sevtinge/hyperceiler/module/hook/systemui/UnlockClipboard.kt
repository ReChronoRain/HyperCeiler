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
package com.sevtinge.hyperceiler.module.hook.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*

object UnlockClipboard : BaseHook() {
    override fun init() {
        // hook 点来自 淡い夏
        // 解锁原生剪切板编辑框 和 截屏编辑框
        var hook: XC_MethodHook.Unhook? = null

        loadClass("com.android.systemui.clipboardoverlay.ClipboardListener").methodFinder()
            .filterByName("start")
            .first().createHook {
                before {
                    hook = if (isMoreAndroidVersion(34)) {
                        // 给正常 Android 14 HyperOS 用户用的
                        loadClass("com.miui.systemui.modulesettings.DeveloperSettings\$Companion").methodFinder()
                            .filterByName("isMiuiOptimizationEnabled")
                            .first().createHook {
                                returnConstant(false)
                            }
                    } else {
                        // 给正常 Android 13 用户用的
                        loadClass("com.miui.systemui.SettingsManager").methodFinder()
                            .filterByName("getMiuiOptimizationEnabled")
                            .first().createHook {
                                returnConstant(false)
                            }
                    }
                }
                after {
                    // 必须 unhook()
                    // 否则会出现各种奇怪的问题
                    hook?.unhook()
                }
            }

        // 14u 泄露包的 hook 点，需要改写 getPrimaryClip，目前没啥法子，等后续有人来写
        /*loadClass("com.android.systemui.clipboardoverlay.ClipboardListener").methodFinder()
            .filterByName("onPrimaryClipChanged")
            .first().createHook {
                before {
                    val getClipboardManager = ObjectUtils.getObjectOrNull(it.thisObject, "mClipboardManager") ?: return@before
                    val getPrimaryClip = getClipboardManager.callMethod("getPrimaryClip")
                    logE(TAG, "getPrimaryClip is $getPrimaryClip")
                }
            }*/
    }
}