package com.sevtinge.cemiuiler.module.home.recent

import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object RecentText : BaseHook() {
    override fun init() {
        val emptyViewText = mPrefsMap.getString("home_recent_text", "")
        if (emptyViewText != "") {
            "com.miui.home.recents.views.RecentsView".hookAfterMethod(
                "showEmptyView", Int::class.javaPrimitiveType
            ) {
                (it.thisObject.getObjectField("mEmptyView") as TextView).apply {
                    this.text = emptyViewText
                }
            }
        }
    }
}