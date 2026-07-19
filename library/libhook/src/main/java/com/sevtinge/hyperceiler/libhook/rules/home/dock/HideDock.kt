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
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook


object HideDock : BaseHook() {
    override fun init() {
        // 上滑时忽略 dock,直接触发最近任务手势
        loadClass("com.miui.home.recents.GestureTouchEventTracker").findMethod { name("isTouchCountAndHotSeatSupport") }
            .createHook {
                returnConstant(false)
            }

        // 拦截dock出现动画
        loadClass("com.miui.home.launcher.dock.DockStateMachine").findMethod { name($$"transitionToAppearingState$default") }
            .createHook {
                replace { }
            }
    }
}
