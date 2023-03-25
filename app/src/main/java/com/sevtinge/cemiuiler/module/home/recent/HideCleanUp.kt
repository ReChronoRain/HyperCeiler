package com.sevtinge.cemiuiler.module.home.recent

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

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