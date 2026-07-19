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
package com.sevtinge.hyperceiler.libhook.rules.home.recent

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object HideStatusBarWhenEnterRecent : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        loadClass("com.miui.home.launcher.common.StatusBarUtils").findMethod { name("isHideStatusBarWhenEnterRecents") }.createHook {
                returnConstant(true)
            }

        loadClass("com.miui.home.launcher.DeviceConfig").findMethod { name("keepStatusBarShowingForBetterPerformance") }.createHook {
                returnConstant(false)
            }
    }

    override fun initBase() {
        // 不应该在默认情况下强制显示
        // if (PrefsBridge.getBoolean("home_recent_hide_status_bar_in_task_view")) {
        loadClass("com.miui.home.launcher.common.DeviceLevelUtils").findMethod { name("isHideStatusBarWhenEnterRecents") }.createHook {
                returnConstant(true)
            }

        loadClass("com.miui.home.launcher.DeviceConfig").findMethod { name("keepStatusBarShowingForBetterPerformance") }.createHook {
                returnConstant(false)
            }
        // } else {
        //     mDeviceLevelClass.findMethod {
        //         name("isHideStatusBarWhenEnterRecents")
        //     }.createHook {
        //         returnConstant(false)
        //     }
        // }
    }
}
