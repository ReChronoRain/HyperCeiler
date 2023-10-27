package com.sevtinge.hyperceiler.module.hook.home.recent

import android.graphics.RectF
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.callStaticMethod
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookAfterMethod

object TaskViewHorizontal : BaseHook() {
    override fun init() {

        val value1 = mPrefsMap.getInt("task_view_horizontal1", 100).toFloat() / 100
        val value2 = mPrefsMap.getInt("task_view_horizontal2", 100).toFloat() / 100
        if (value1 == 1f && value2 == 1f) return
        "com.miui.home.recents.views.TaskStackViewsAlgorithmHorizontal".hookAfterMethod(
            "scaleTaskView", RectF::class.java,
        ) {
            "com.miui.home.recents.util.Utilities".findClass().callStaticMethod(
                "scaleRectAboutCenter",
                it.args[0],
                if (it.thisObject.callMethod("isLandscapeVisually") as Boolean) value2 else value1
            )
        }

    }
}
