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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework

import android.content.Context
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.api.field


object RemoveSmallWindowRestrictions : BaseHook() {
    private val mSettingsClass by lazy {
        loadClass("com.android.server.wm.WindowManagerService\$SettingsObserver")
    }
    private val mWindowsUtilsClass by lazy {
        loadClass("android.util.MiuiMultiWindowUtils")
    }
    private val mWindowsClass by lazy {
        loadClass("android.util.MiuiMultiWindowAdapter")
    }

    override fun init() {
        runCatching {
            loadClass("com.android.server.wm.ActivityTaskManagerService").methodFinder()
                .filterByName("retrieveSettings")
                .single().createHook {
                    after { param ->
                        param.thisObject.javaClass.field("mDevEnableNonResizableMultiWindow")
                            .setBoolean(param.thisObject, true)
                    }
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook retrieveSettings failed by: $e")
        }

        runCatching {
            mSettingsClass.methodFinder().filter {
                name == "updateDevEnableNonResizableMultiWindow"
            }.toList().createHooks {
                after { param ->
                    val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                    val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                    mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow")
                        .setBoolean(mAtmService, true)
                }
            }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook updateDevEnableNonResizableMultiWindow failed by: $e")
        }

        runCatching {
            mSettingsClass.methodFinder().filter {
                name == "onChange"
            }.toList().createHooks {
                after { param ->
                    val this0 = param.thisObject.javaClass.field("this\$0").get(param.thisObject)
                    val mAtmService = this0.javaClass.field("mAtmService").get(this0)
                    mAtmService.javaClass.field("mDevEnableNonResizableMultiWindow")
                        .setBoolean(mAtmService, true)
                }
            }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook onChange failed by: $e")
        }

        runCatching {
            mWindowsUtilsClass.methodFinder()
                .filterByName("isForceResizeable")
                .first().createHook {
                    returnConstant(true)
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook isForceResizeable failed by: $e")
        }

        // Author: LittleTurtle2333
        runCatching {
            loadClass("com.android.server.wm.Task").methodFinder()
                .filterByName("isResizeable")
                .first().createHook {
                    returnConstant(true)
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook isResizeable failed by: $e")
        }

        runCatching {
            mWindowsClass.methodFinder()
                .filterByName("getFreeformBlackList")
                .single().createHook {
                    returnConstant(mutableListOf<String>())
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook getFreeformBlackList failed by: $e")
        }

        runCatching {
            mWindowsClass.methodFinder()
                .filterByName("getFreeformBlackListFromCloud")
                .filterByParamTypes {
                    it[0] == Context::class.java
                }
                .single().createHook {
                    returnConstant(mutableListOf<String>())
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook getFreeformBlackListFromCloud failed by: $e")
        }

        runCatching {
            mWindowsClass.methodFinder()
                .filterByName("getStartFromFreeformBlackListFromCloud")
                .single().createHook {
                    returnConstant(mutableListOf<String>())
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook getStartFromFreeformBlackListFromCloud failed by: $e")
        }

        runCatching {
            mWindowsUtilsClass.methodFinder()
                .filterByName("supportFreeform")
                .single().createHook {
                    returnConstant(true)
                }
        }.onFailure { e ->
            logE(TAG, this.lpparam.packageName, "Hook supportFreeform failed by: $e")
        }

    }

}
