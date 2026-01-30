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
package com.sevtinge.hyperceiler.libhook.rules.home.folder

import android.widget.GridView
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods

object FolderVerticalSpacing : HomeBaseHookNew() {

    private val verticalSpacing by lazy {
        mPrefsMap.getInt("home_folder_vertical_spacing", 0)
    }


    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        if (verticalSpacing <= 0) return
        findClass("com.miui.home.folder.FolderView")
            .hookAllMethods("bind") {
                after {
                    val mContent = getObjectField(it.thisObject, "mContent") as GridView
                    mContent.verticalSpacing = dp2px(verticalSpacing.toFloat())
                }
            }
    }


    override fun initBase() {
        if (verticalSpacing <= 0) return
        findClass("com.miui.home.launcher.Folder").hookAllMethods("bind") {
            after {
                val mContent = getObjectField(it.thisObject, "mContent") as GridView
                mContent.verticalSpacing = dp2px(verticalSpacing.toFloat())
            }
        }

    }
}
