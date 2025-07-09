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
package com.sevtinge.hyperceiler.hook.module.hook.home.mipad

import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object EnableMoreSetting : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.settings.MiuiHomeSettings").methodFinder()
            .filterByName("checkDevice").first().createHook {
                returnConstant(true)
            }

        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder()
            .filterByName("needShowCellsEntry").first().createHook {
                returnConstant(true)
            }

        loadClass("com.miui.home.launcher.LauncherMenu").methodFinder()
            .filterByName("onShow").first().createHook {
                after {
                    val mDefaultScreenPreview =
                        it.thisObject.getObjectField("mDefaultScreenPreview") as View
                    mDefaultScreenPreview.visibility = View.VISIBLE
                }
            }
    }
}
