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
package com.sevtinge.hyperceiler.module.hook.home.drawer

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethodAs
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.hookAfterMethod

object AppDrawer : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("home_drawer_all")) {
            try {
                loadClassOrNull("com.miui.home.launcher.allapps.category.BaseAllAppsCategoryListContainer")!!
                    .methodFinder()
                    .first {
                        name == "buildSortCategoryList"
                    }
            } catch (e: Exception) {
                loadClass("com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer").methodFinder()
                    .first {
                        name == "buildSortCategoryList"
                    }
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

        if (mPrefsMap.getBoolean("home_drawer_editor")) {
            "com.miui.home.launcher.allapps.AllAppsGridAdapter".hookAfterMethod(
                "onBindViewHolder",
                "com.miui.home.launcher.allapps.AllAppsGridAdapter.ViewHolder".findClass(),
                Int::class.javaPrimitiveType
            ) {
                if (it.args[0].callMethodAs<Int>("getItemViewType") == 64) {
                    it.args[0].getObjectFieldAs<View>("itemView").visibility = View.INVISIBLE
                }
            }
        }

    }
}
