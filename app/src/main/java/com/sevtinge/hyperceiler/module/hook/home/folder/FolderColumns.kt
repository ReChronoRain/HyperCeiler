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

import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.home.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.log.*
import de.robv.android.xposed.XposedHelpers

object FolderColumns : BaseHook() {
    override fun init() {

        val columns = mPrefsMap.getInt("home_folder_columns", 3)

        "com.miui.home.launcher.Folder".findClass().hookAfterAllMethods(
            "bind"
        ) {
            val folder = it.thisObject as ViewGroup

            val mBackgroundView = folder.getObjectField("mBackgroundView") as ViewGroup
            val mHeader = folder.getObjectField("mHeader") as View
            val mTitleText = folder.getObjectField("mTitleText") as TextView
            val mFolderGrid = folder.getObjectField("mFolderGrid") as FrameLayout
            val mContent = folder.getObjectField("mContent") as GridView
            mContent.numColumns = columns
            if (mPrefsMap.getBoolean("home_folder_width")) {
                mBackgroundView.setPadding(0, 0, 0, 0)
                mFolderGrid.layoutParams = mFolderGrid.layoutParams.also { lp ->
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                mContent.layoutParams = mContent.layoutParams.also { lp ->
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                mHeader.layoutParams = mHeader.layoutParams.also { lp ->
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                }

                val sidePadding = if (mPrefsMap.getBoolean("home_folder_horizontal_padding_enable")) {
                    DisplayUtils.dp2px(
                        mPrefsMap.getInt("home_folder_horizontal_padding", 0).toFloat()
                    )
                } else {
                    -1
                }
                if (sidePadding != -1) {
                    mFolderGrid.setPadding(sidePadding, mFolderGrid.paddingTop, sidePadding, mFolderGrid.paddingBottom)
                    mContent.setPadding(sidePadding, mContent.paddingTop, sidePadding, mContent.paddingBottom)
                    mHeader.setPadding(sidePadding, mFolderGrid.paddingTop, sidePadding, mHeader.paddingBottom)
                    mTitleText.setPadding(
                        DisplayUtils.dp2px(14F) + sidePadding,
                        mTitleText.paddingTop,
                        mTitleText.paddingRight + sidePadding,
                        mTitleText.paddingBottom
                    )
                } else {
                    mTitleText.setPadding(
                        DisplayUtils.dp2px(14F),
                        mTitleText.paddingTop,
                        mTitleText.paddingRight,
                        mTitleText.paddingBottom
                    )
                }
            }
        }

    }
}
