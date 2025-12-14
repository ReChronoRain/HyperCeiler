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
package com.sevtinge.hyperceiler.hook.module.rules.aod

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object UnlockShortCuts: BaseHook() {
    var list: MutableList<String> = mutableListOf()

    override fun init() {
        loadClass("com.miui.keyguard.shortcuts.utils.DataUtils")
            .methodFinder()
            .filterByName("loadWhiteItems")
            .first()
            .createAfterHook {
                val originalResult = it.result as? List<*> ?: return@createAfterHook
                list.clear()

                originalResult.forEach { item ->
                    if (item is String) {
                        list.add(item)
                    }
                }

                list.add("com.sevtinge.hyperceiler-HYPERCEILER")
                it.result = list
            }
    }

}
