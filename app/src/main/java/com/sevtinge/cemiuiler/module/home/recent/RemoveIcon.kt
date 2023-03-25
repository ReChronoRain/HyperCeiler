package com.sevtinge.cemiuiler.module.home.recent

import android.view.View
import android.widget.ImageView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object RemoveIcon : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("home_recent_remove_icon")) {
            "com.miui.home.recents.views.TaskViewHeader".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mImage = it.thisObject.getObjectField("mIconView") as ImageView
                mImage.visibility = View.GONE
            }
        }
    }
}