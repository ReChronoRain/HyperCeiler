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
package com.sevtinge.hyperceiler.libhook.rules.home.drawer

import android.view.View
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs

object AppDrawer : BaseHook() {
    override fun init() {
        if (PrefsBridge.getBoolean("home_drawer_all")) {
            try {
                loadClassOrNull("com.miui.home.launcher.allapps.category.BaseAllAppsCategoryListContainer")!!
                    .findMethod { name("buildSortCategoryList") }
            } catch (_: Exception) {
                loadClassOrNull("com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer")!!
                    .findMethod { name("buildSortCategoryList") }
            }.createHook {
                after {
                    val list = it.result as ArrayList<*>
                    if (list.size > 1) {
                        list.removeAt(0)
                        it.result = list
                    }
                }
            }
        }

        if (PrefsBridge.getBoolean("home_drawer_editor")) {
            findClass("com.miui.home.launcher.allapps.AllAppsGridAdapter").findMethod {
                name("onBindViewHolder")
            }.createAfterHook {
                if ((it.args[0]?.callMethod("getItemViewType") as? Int) == 64) {
                    it.args[0]?.getObjectFieldAs<View>("itemView")?.visibility = View.INVISIBLE
                }
            }
        }

    }
}
