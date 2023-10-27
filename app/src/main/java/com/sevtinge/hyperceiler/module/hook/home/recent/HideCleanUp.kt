package com.sevtinge.hyperceiler.module.hook.home.recent

import android.view.View
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

object HideCleanUp : BaseHook() {
    override fun init() {
        val recentsContainerClass = "com.miui.home.recents.views.RecentsContainer".findClass()
        if (mPrefsMap.getBoolean("home_recent_hide_clean_up")) {
            recentsContainerClass.hookAfterMethod(
                "onFinishInflate"
            ) {
                val mView = it.thisObject.getObjectField("mClearAnimView") as View
                mView.visibility = View.GONE
            }
        }
    }
}
