package com.sevtinge.cemiuiler.module.hook.home.recent

import android.graphics.RectF
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callStaticMethod
import com.sevtinge.cemiuiler.utils.findClass
import com.sevtinge.cemiuiler.utils.replaceMethod

object TaskViewVertical : BaseHook() {
    override fun init() {

        val value = mPrefsMap.getInt("home_recent_vertical_task_view_card_size", 100).toFloat() / 100
        if (value == -1f || value == 1f) return
        "com.miui.home.recents.views.TaskStackViewsAlgorithmVertical".replaceMethod(
            "scaleTaskView", RectF::class.java
        ) {
            "com.miui.home.recents.util.Utilities".findClass().callStaticMethod(
                "scaleRectAboutCenter",
                it.args[0],
                value * "com.miui.home.recents.util.Utilities".findClass()
                    .callStaticMethod("getTaskViewScale", appContext) as Float
            )
        }

    }
}
