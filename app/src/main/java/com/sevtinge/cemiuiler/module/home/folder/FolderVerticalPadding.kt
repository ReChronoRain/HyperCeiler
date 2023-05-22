package com.sevtinge.cemiuiler.module.home.folder

import android.widget.GridView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.dp2px
import com.sevtinge.cemiuiler.utils.findClass
import com.sevtinge.cemiuiler.utils.hookAfterAllMethods
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