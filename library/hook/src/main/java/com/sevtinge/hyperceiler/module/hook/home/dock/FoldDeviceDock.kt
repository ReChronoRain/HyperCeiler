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
package com.sevtinge.hyperceiler.module.hook.home.dock

import android.content.Context
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import com.sevtinge.hyperceiler.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook

object FoldDeviceDock : BaseHook() {
    private val mHotSeatsClass by lazy {
        loadClass("com.miui.home.launcher.hotseats.HotSeats")
    }

    override fun init() {
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null
        var hook3: XC_MethodHook.Unhook? = null

        mHotSeatsClass.methodFinder()
            .filterByName("initContent")
            .single().createHook {
                before {
                    hook1 = "com.miui.home.launcher.DeviceConfig".hookBeforeMethod(
                        "isFoldDevice"
                    ) { hookParam ->
                        hookParam.result = true
                    }
                }
                after {
                    hook1?.unhook()
                }
            }

        try {
            mHotSeatsClass.methodFinder()
                .filterByName("updateContent")
                .single()
        } catch (e: Exception) {
            mHotSeatsClass.methodFinder()
                .filterByName("updateContentView")
                .single()
        }.createHook {
            before {
                hook2 = "com.miui.home.launcher.Application".hookBeforeMethod(
                    "isInFoldLargeScreen"
                ) { hookParam ->
                    hookParam.result = true
                }
            }
            after {
                hook2?.unhook()
            }
        }

        mHotSeatsClass.methodFinder()
            .filterByName("isNeedUpdateItemInfo")
            .single().createHook {
                before {
                    hook3 = "com.miui.home.launcher.Application".hookBeforeMethod(
                        "isInFoldLargeScreen"
                    ) { hookParam -> hookParam.result = true }
                }
                after {
                    hook3?.unhook()
                }
            }

        "com.miui.home.launcher.DeviceConfig".hookAfterMethod(
            "getHotseatMaxCount"
        ) {
            it.result = mPrefsMap.getInt("home_fold_dock_hotseat", 3)
        }

        "com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider".hookBeforeMethod(
            "getLimitCount"
        ) {
            it.result = mPrefsMap.getInt("home_fold_dock_run", 2)
        }

        "com.miui.home.launcher.allapps.LauncherMode".hookBeforeMethod(
            "isHomeSupportSearchBar",
            Context::class.java
        ) {
            it.result = false
        }
    }
}
