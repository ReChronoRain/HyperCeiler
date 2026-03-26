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
package com.sevtinge.hyperceiler.hook.module.rules.home.folder

import android.widget.GridView
import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.hookAfterAllMethods
import de.robv.android.xposed.XposedHelpers

object FolderVerticalSpacing : HomeBaseHookNew() {

    private val verticalSpacing by lazy {
        mPrefsMap.getInt("home_folder_vertical_spacing", 0)
    }


    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        if (verticalSpacing <= 0) return
        "com.miui.home.folder.FolderView".findClass().hookAfterAllMethods(
            "bind"
        ) {
            val mContent = XposedHelpers.getObjectField(it.thisObject, "mContent") as GridView
            mContent.verticalSpacing = dp2px(verticalSpacing.toFloat())
        }
    }


    override fun initBase() {
        if (verticalSpacing <= 0) return
        "com.miui.home.launcher.Folder".findClass().hookAfterAllMethods(
            "bind"
        ) {
            val mContent = XposedHelpers.getObjectField(it.thisObject, "mContent") as GridView
            mContent.verticalSpacing = dp2px(verticalSpacing.toFloat())
        }

    }
}
