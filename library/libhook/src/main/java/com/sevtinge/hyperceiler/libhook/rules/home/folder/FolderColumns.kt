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

import android.graphics.Rect
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import androidx.core.view.isNotEmpty
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllMethods
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setPadding
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setPaddingLeft
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setPaddingSide

object FolderColumns : HomeBaseHookNew() {
    private const val FOLDER = "com.miui.home.launcher.Folder"
    private const val FOLDER_NEW = "com.miui.home.folder.FolderView"
    private const val ANIM_CONTROLLER = "com.miui.home.launcher.folder.FolderAnimController"
    private const val ANIM_CONTROLLER_NEW = "com.miui.home.folder.FolderAnimController"

    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        val titlePosition = mPrefsMap.getStringAsInt("home_folder_title_pos", 0)
        val isFullScreenWidthHook = mPrefsMap.getBoolean("home_folder_width")
        val isHorPaddingHook = mPrefsMap.getBoolean("home_folder_horizontal_padding_enable")
        val columns = mPrefsMap.getInt("home_folder_columns", 3)

        if (columns != 3) {
            findClassIfExists(ANIM_CONTROLLER_NEW)?.let {
                it.hookAllMethods("setupView") {
                    after { param ->
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
        }

        findClassIfExists(FOLDER_NEW).hookAllMethods("onOpen") {
            after { param ->
                param.thisObject.getObjectFieldAs<TextView>("mTitleText").also { mTitleText ->
                    if (mTitleText.ellipsize == TextUtils.TruncateAt.MARQUEE) {
                        return@also
                    }

                    mTitleText.ellipsize = TextUtils.TruncateAt.MARQUEE
                    mTitleText.isHorizontalFadingEdgeEnabled = true
                    mTitleText.setSingleLine()
                    mTitleText.marqueeRepeatLimit = -1
                    mTitleText.isSelected = true
                    mTitleText.setHorizontallyScrolling(true)
                }

            }
        }

        val firstItemRect = Rect()
        findClassIfExists(FOLDER_NEW).hookAllMethods("bind") {
            after { param ->
                val folder = param.thisObject as ViewGroup

                val mBackgroundView = folder.getObjectFieldAs<ViewGroup>("mBackgroundView")
                val mHeader = folder.getObjectFieldAs<View>("mHeader")
                val mTitleText = folder.getObjectFieldAs<TextView>("mTitleText")
                val mRenameEdit = folder.getObjectFieldAs<EditText>("mRenameEdit")
                val mContent = folder.getObjectFieldAs<GridView>("mContent")
                mContent.numColumns = columns

                if (isFullScreenWidthHook) {
                    mBackgroundView.setPadding(0)

                    val setFullScreenWidth = { view: View ->
                        view.layoutParams = view.layoutParams.also { lp ->
                            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }

                    setFullScreenWidth(mContent)
                    setFullScreenWidth(mHeader)
                    setFullScreenWidth(mTitleText)
                    setFullScreenWidth(mRenameEdit)

                    var sidePadding = 0
                    if (isHorPaddingHook) {
                        sidePadding = DisplayUtils.dp2px(
                            mPrefsMap.getInt("home_folder_horizontal_padding", 0).toFloat()
                        )
                        mContent.setPaddingSide(sidePadding)
                        mHeader.setPaddingSide(sidePadding)
                    }

                    if (titlePosition == 0) {
                        val setTitlePadding = {
                            if (firstItemRect.left >= sidePadding) {
                                val configClass = findClassIfExists(DEVICE_CONFIG_NEW)
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
                                        if (mContent.isNotEmpty() &&
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

    @Version(isPad = true, min = 450000000)
    private fun initPadHook() {
        val titlePosition = mPrefsMap.getStringAsInt("home_folder_title_pos", 0)
        val isFullScreenWidthHook = mPrefsMap.getBoolean("home_folder_width")
        val isHorPaddingHook = mPrefsMap.getBoolean("home_folder_horizontal_padding_enable")
        val columns = mPrefsMap.getInt("home_folder_columns", 4)

        findClassIfExists(FOLDER).hookAllMethods("onOpen") {
            after { param ->
                param.thisObject.getObjectFieldAs<TextView>("mTitleText").also { mTitleText ->
                    if (mTitleText.ellipsize == TextUtils.TruncateAt.MARQUEE) {
                        return@also
                    }

                    mTitleText.ellipsize = TextUtils.TruncateAt.MARQUEE
                    mTitleText.isHorizontalFadingEdgeEnabled = true
                    mTitleText.setSingleLine()
                    mTitleText.marqueeRepeatLimit = -1
                    mTitleText.isSelected = true
                    mTitleText.setHorizontallyScrolling(true)
                }
            }
        }

        val firstItemRect = Rect()
        findClassIfExists(FOLDER).hookAllMethods("bind") {
            after { param ->
                val folder = param.thisObject as ViewGroup

                val mBackgroundView = folder.getObjectFieldAs<ViewGroup>("mBackgroundView")
                val mHeader = folder.getObjectFieldAs<View>("mHeader")
                val mTitleText = folder.getObjectFieldAs<TextView>("mTitleText")
                val mRenameEdit = folder.getObjectFieldAs<EditText>("mRenameEdit")
                val mContent = folder.getObjectFieldAs<GridView>("mContent")
                mContent.numColumns = columns

                if (isFullScreenWidthHook) {
                    mBackgroundView.setPadding(0)

                    val setFullScreenWidth = { view: View ->
                        view.layoutParams = view.layoutParams.also { lp ->
                            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }

                    setFullScreenWidth(mContent)
                    setFullScreenWidth(mHeader)
                    setFullScreenWidth(mTitleText)
                    setFullScreenWidth(mRenameEdit)

                    var sidePadding = 0
                    if (isHorPaddingHook) {
                        sidePadding = if (mContent.resources.configuration.orientation ==
                            android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        ) {
                            DisplayUtils.dp2px(
                                mPrefsMap.getInt("home_folder_horizontal_padding_pad_h", 0)
                                    .toFloat()
                            )
                        } else {
                            DisplayUtils.dp2px(
                                mPrefsMap.getInt("home_folder_horizontal_padding_pad_v", 0)
                                    .toFloat()
                            )
                        }
                        mContent.setPaddingSide(sidePadding)
                        mHeader.setPaddingSide(sidePadding)
                    }

                    if (titlePosition == 0) {
                        val setTitlePadding = {
                            if (firstItemRect.left >= sidePadding) {
                                val configClass = findClassIfExists(DEVICE_CONFIG_OLD)
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
                                        if (mContent.isNotEmpty() &&
                                            mContent.getChildAt(mContent.childCount - 1).bottom <= mContent.bottom
                                        ) {
                                            mContent.getChildAt(0)
                                                .getGlobalVisibleRect(firstItemRect)
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

    override fun initBase() {
        val titlePosition = mPrefsMap.getStringAsInt("home_folder_title_pos", 0)
        val isFullScreenWidthHook = mPrefsMap.getBoolean("home_folder_width")
        val isHorPaddingHook = mPrefsMap.getBoolean("home_folder_horizontal_padding_enable")
        val columns = mPrefsMap.getInt("home_folder_columns", 3)

        if (isPad()) {
            // 你米的代码是真的老啊......
            initPadHook()
            return
        }

        if (columns != 3) {
            findClassIfExists(ANIM_CONTROLLER).hookAllMethods("setupView") {
                after {
                        param ->
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

        findClassIfExists(FOLDER).hookAllMethods("onOpen") {
            after { param ->
                param.thisObject.getObjectFieldAs<TextView>("mTitleText").also { mTitleText ->
                    if (mTitleText.ellipsize == TextUtils.TruncateAt.MARQUEE) {
                        return@also
                    }

                    mTitleText.ellipsize = TextUtils.TruncateAt.MARQUEE
                    mTitleText.isHorizontalFadingEdgeEnabled = true
                    mTitleText.setSingleLine()
                    mTitleText.marqueeRepeatLimit = -1
                    mTitleText.isSelected = true
                    mTitleText.setHorizontallyScrolling(true)
                }
            }
        }

        val firstItemRect = Rect()
        findClassIfExists(FOLDER).hookAllMethods("bind") {
            after { param ->
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

                    if (titlePosition == 0) {
                        val setTitlePadding = {
                            if (firstItemRect.left >= sidePadding) {
                                val configClass = findClassIfExists(DEVICE_CONFIG_OLD)
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
                                        if (mContent.isNotEmpty() &&
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

    /*@Version(isPad = false, max = 539309777)
    private fun initOldHook() {
        val value = mPrefsMap.getInt("home_folder_columns", 3)
        if (value == 3) return
        "com.miui.home.launcher.Folder".findClass().hookAfterAllMethods(
            "bind"
        ) {
            val columns: Int = value
            val mContent = XposedHelpers.getObjectField(it.thisObject, "mContent") as GridView
            mContent.numColumns = columns
            if (mPrefsMap.getBoolean("home_folder_width") && (columns > 3)) {
                mContent.setPadding(0, 0, 0, 0)
                val lp = mContent.layoutParams
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                mContent.layoutParams = lp
            }
            if (columns > 3) {
                val mBackgroundView = XposedHelpers.getObjectField(it.thisObject, "mBackgroundView") as ViewGroup
                mBackgroundView.setPadding(
                    mBackgroundView.paddingLeft / 3,
                    mBackgroundView.paddingTop,
                    mBackgroundView.paddingRight / 3,
                    mBackgroundView.paddingBottom
                )
            }
        }
    }*/
}
