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
package com.sevtinge.hyperceiler.module.hook.home.widget

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import java.util.function.Predicate

object HideWidgetTitles : BaseHook() {
    override fun init() {

        val maMlWidgetInfo = loadClass("com.miui.home.launcher.maml.MaMlWidgetInfo")
        loadClass("com.miui.home.launcher.LauncherAppWidgetHost").methodFinder().first {
            name == "createLauncherWidgetView" && parameterCount == 4
        }.createHook {
            after {
                val view = it.result as Any
                view.callMethod("getTitleView")?.callMethod("setVisibility", View.GONE)
            }
        }

        "com.miui.home.launcher.Launcher".hookAfterMethod(
            "addMaMl", maMlWidgetInfo, Boolean::class.java, Predicate::class.java
        ) {
            val view = it.result as Any
            view.callMethod("getTitleView")?.callMethod("setVisibility", View.GONE)
        }

    }
}
