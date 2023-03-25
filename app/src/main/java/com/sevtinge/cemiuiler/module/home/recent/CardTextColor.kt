package com.sevtinge.cemiuiler.module.home.recent

import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object CardTextColor : BaseHook() {
    override fun init() {
        val recentTextColor = mPrefsMap.getInt("home_recent_text_color", -1)
        if (recentTextColor != -1) {
            val taskViewHeaderClass = "com.miui.home.recents.views.TaskViewHeader".findClass()
            taskViewHeaderClass.hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleView") as TextView
                mTitle.setTextColor(recentTextColor)
            }
        }
    }
}