package com.sevtinge.hyperceiler.module.hook.home.recent

import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

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
