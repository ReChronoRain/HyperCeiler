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
package com.sevtinge.hyperceiler.module.hook.home.folder

import android.widget.GridView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookAfterAllMethods
import de.robv.android.xposed.XposedHelpers

object FolderVerticalPadding : BaseHook() {
    override fun init() {

        val verticalPadding = mPrefsMap.getInt("home_folder_vertical_padding", 0)
        if (verticalPadding <= 0) return
        "com.miui.home.launcher.Folder".findClass().hookAfterAllMethods(
            "bind"
        ) {
            val mContent = XposedHelpers.getObjectField(it.thisObject, "mContent") as GridView
            mContent.verticalSpacing = dp2px(verticalPadding.toFloat())
        }

    }
}
