package com.sevtinge.cemiuiler.module.home.recent

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.hookAfterAllConstructors
import com.sevtinge.cemiuiler.utils.woobox.setIntField

object FreeformCardBackgroundColor : BaseHook() {
    override fun init() {
        val appCardBgColor = mPrefsMap.getInt("home_recent_freeform_background_color", -1)
        if (appCardBgColor != -1) {
            "com.miui.home.recents.views.TaskViewThumbnail".findClass().hookAfterAllConstructors {
                it.thisObject.setIntField("mBgColorForSmallWindow", appCardBgColor)
            }
        }
    }
}