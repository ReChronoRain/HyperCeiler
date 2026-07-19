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

import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object AnimDurationRatio : BaseHook() {
    override fun init() {
        var value1 = PrefsBridge.getInt("home_title_animation_speed", 100).toFloat()
        var value2 = PrefsBridge.getInt("home_recent_animation_speed", 100).toFloat()
        if (value1 != 100f) {
            value1 /= 100f
            loadClass("com.miui.home.recents.util.RectFSpringAnim").findMethod { name("getModifyResponse") }.createHook {
                    before {
                        it.result = it.args[0] as Float * value1
                    }
                }
        }
        if (value2 != 100f) {
            value2 /= 100f
            loadClass("com.miui.home.launcher.common.DeviceLevelUtils").findMethod { name("getDeviceLevelTransitionAnimRatio") }.createHook {
                    before {
                        it.result = value2
                    }
                }
        }
    }
}
