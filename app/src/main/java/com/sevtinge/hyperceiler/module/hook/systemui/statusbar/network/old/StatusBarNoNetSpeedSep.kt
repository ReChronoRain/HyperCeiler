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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old

import android.annotation.SuppressLint
import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers


@SuppressLint("StaticFieldLeak")
object StatusBarNoNetSpeedSep : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.views.NetworkSpeedSplitter", lpparam.classLoader).methodFinder()
            .first {
                name == "updateVisibility"
            }.createHook {
            before {
                XposedHelpers.setObjectField(it.thisObject, "mNetworkSpeedVisibility", View.GONE)
            }
        }
    }
}
