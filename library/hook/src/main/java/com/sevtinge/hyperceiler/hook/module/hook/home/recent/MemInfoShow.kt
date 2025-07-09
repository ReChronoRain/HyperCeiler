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
package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object MemInfoShow : BaseHook() {
    override fun init() {
        // hyperOS for Pad 修复方案来自 hyper helper
        runCatching {
            // 此方法调用会将内存显示 hide，需拦截
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder()
                .filterByName("hideTxtMemoryInfoView")
                .single().createHook {
                returnConstant(null)
            }
        }.onFailure {
            logE(TAG, "hideTxtMemoryInfoView method is null")
        }

        try {
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder()
                .filterByName("isMemInfoShow")
                .single()
        } catch (_: Throwable) {
            loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder()
                .filterByName("canTxtMemInfoShow")
                .single()
        }.createHook {
            returnConstant(true)
        }
    }
}
