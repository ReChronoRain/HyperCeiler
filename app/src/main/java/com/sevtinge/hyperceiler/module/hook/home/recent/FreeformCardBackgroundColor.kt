package com.sevtinge.hyperceiler.module.hook.home.recent

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookAfterAllConstructors
import com.sevtinge.hyperceiler.utils.setIntField

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
