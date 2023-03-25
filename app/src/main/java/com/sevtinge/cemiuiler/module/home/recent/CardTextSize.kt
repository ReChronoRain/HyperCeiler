package com.sevtinge.cemiuiler.module.home.recent

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object CardTextSize : BaseHook() {
    override fun init() {
        val recentTextSize = mPrefsMap.getInt("home_recent_text_size", -1)
        if (recentTextSize != -1) {
            val taskViewHeaderClass = "com.miui.home.recents.views.TaskViewHeader".findClass()
            taskViewHeaderClass.hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleView") as TextView
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, recentTextSize.toFloat())
                if (recentTextSize == 0) mTitle.visibility = View.GONE
            }
        }
    }
}