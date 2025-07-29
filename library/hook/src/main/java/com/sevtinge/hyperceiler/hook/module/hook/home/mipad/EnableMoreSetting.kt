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
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectField

object EnableMoreSetting : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.settings.MiuiHomeSettings").methodFinder().first{
            name == "checkDevice"
        }.createHook{
            returnConstant(true)
        }

        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder().first{
            name == "needShowCellsEntry"
        }.createHook{
            returnConstant(true)
        }

        loadClass("com.miui.home.launcher.LauncherMenu").methodFinder().first{
            name == "onShow"
        }.createHook{
            after{
                val mDefaultScreenPreview = it.thisObject.getObjectField("mDefaultScreenPreview") as View
                mDefaultScreenPreview.visibility = View.VISIBLE
            }
        }
    }
}
