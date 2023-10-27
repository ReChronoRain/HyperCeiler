package com.sevtinge.hyperceiler.module.hook.home.recent

import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

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
