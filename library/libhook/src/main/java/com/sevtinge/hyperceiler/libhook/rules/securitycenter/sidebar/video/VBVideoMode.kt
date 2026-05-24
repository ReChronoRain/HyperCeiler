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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method

class VBVideoMode : BaseHook() {
    override fun useDexKit() = true
    private lateinit var vbVideoModeMethod: Method

    override fun initDexKit(): Boolean {
        // 开放影院/自定义模式
        vbVideoModeMethod = requiredMember("VBVideoMode") {
            it.findMethod {
                matcher {
                    usingStrings = listOf("TheatreModeUtils")
                    usingNumbers = listOf(32)
                }
            }.single()
        }
        return true
    }

    override fun init() {
        vbVideoModeMethod.createHook {
            returnConstant(true)
        }
    }
}
