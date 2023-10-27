package com.sevtinge.hyperceiler.module.hook.home.recent

import android.view.View
import android.widget.ImageView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

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
