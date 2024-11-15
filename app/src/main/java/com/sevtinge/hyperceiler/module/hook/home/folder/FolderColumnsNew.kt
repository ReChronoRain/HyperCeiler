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

import android.graphics.*
import android.view.*
import android.view.ViewTreeObserver.*
import android.widget.*
import com.sevtinge.hyperceiler.module.hook.home.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.extension.*

object FolderColumnsNew : HomeBaseHook() {
    private const val FOLDER = "com.miui.home.launcher.Folder"
    private const val ANIM_CONTROLLER = "com.miui.home.launcher.folder.FolderAnimController"

    override fun init() {
        if (!isNewHome) {
            return
        }

        val titlePosition = mPrefsMap.getStringAsInt("home_folder_title_pos", 0)
        val isFullScreenWidthHook = mPrefsMap.getBoolean("home_folder_width")
        val isHorPaddingHook = mPrefsMap.getBoolean("home_folder_horizontal_padding_enable")
        val columns = mPrefsMap.getInt("home_folder_columns", 3)

        if (columns != 3) {
            findClassIfExists(ANIM_CONTROLLER)?.let {
                it.hookAfterAllMethods("setupView") { param ->
                    val controller = param.thisObject

                    val mAnimaFolderGridView =
                        controller.getObjectFieldAs<GridView>("mAnimaFolderGridView")
                    controller.setIntField(
                        "DISPLAY_COUNT_MAX",
                        mAnimaFolderGridView.callMethodAs<Int>("getMaxRow") * columns
                    )
                    controller.setIntField("mFolderColumnCount", columns)
                }
            }
        }

        val firstItemRect = Rect()
        findClass(FOLDER).hookAfterAllMethods("bind") { param ->
            val folder = param.thisObject as ViewGroup

            val mBackgroundView = folder.getObjectFieldAs<ViewGroup>("mBackgroundView")
            val mHeader = folder.getObjectFieldAs<View>("mHeader")
            val mTitleText = folder.getObjectFieldAs<TextView>("mTitleText")
            val mRenameEdit = folder.getObjectFieldAs<EditText>("mRenameEdit")
            val mFolderGrid = folder.getObjectFieldAs<FrameLayout>("mFolderGrid")
            val mContent = folder.getObjectFieldAs<GridView>("mContent")
            mContent.numColumns = columns

            if (isFullScreenWidthHook) {
                mBackgroundView.setPadding(0)

                val setFullScreenWidth = { view: View ->
                    view.layoutParams = view.layoutParams.also { lp ->
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }

                setFullScreenWidth(mFolderGrid)
                setFullScreenWidth(mContent)
                setFullScreenWidth(mHeader)
                setFullScreenWidth(mTitleText)
                setFullScreenWidth(mRenameEdit)

                var sidePadding = 0
                if (isHorPaddingHook) {
                    sidePadding = DisplayUtils.dp2px(
                        mPrefsMap.getInt("home_folder_horizontal_padding", 0).toFloat()
                    )
                    mFolderGrid.setPaddingSide(sidePadding)
                    mContent.setPaddingSide(sidePadding)
                    mHeader.setPaddingSide(sidePadding)
                }

                logI(TAG, lpparam.packageName, "$titlePosition - $firstItemRect")
                if (titlePosition == 0) {
                    val setTitlePadding = {
                        if (firstItemRect.left >= sidePadding) {
                            val configClass = findClass(DEVICE_CONFIG)
                            val space = (configClass.callStaticMethodAs<Int>("getCellWidth") -
                                    configClass.callStaticMethodAs<Int>("getIconWidth")) / 2

                            val fixedValue = DisplayUtils.dp2px(4.0f)
                            mTitleText.setPaddingLeft(firstItemRect.left - sidePadding + space + fixedValue)
                            mRenameEdit.setPaddingLeft(firstItemRect.left - sidePadding + space + fixedValue)
                        }
                    }

                    if (firstItemRect.left == 0) {
                        mContent.viewTreeObserver.addOnGlobalLayoutListener(
                            object : OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    if (mContent.childCount > 0 &&
                                        mContent.getChildAt(mContent.childCount - 1).bottom <= mContent.bottom
                                    ) {
                                        mContent.getChildAt(0).getGlobalVisibleRect(firstItemRect)
                                        setTitlePadding()
                                    }

                                    mContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                }
                            }
                        )
                    } else {
                        setTitlePadding()
                    }
                }
            }

            if (titlePosition == 1) {
                mTitleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                mRenameEdit.textAlignment = View.TEXT_ALIGNMENT_CENTER

                mTitleText.setPaddingLeft(mTitleText.paddingRight)
                mRenameEdit.setPaddingLeft(mRenameEdit.paddingRight)
            }
        }
    }
}
