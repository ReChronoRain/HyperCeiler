package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod

object TaskViewHeight : BaseHook() {
    override fun init() {
        var taskViewHeightValue =
            mPrefsMap.getInt("home_recent_task_view_height", 52).toFloat() / 100

        loadClass("com.miui.home.recents.layoutconfig.TaskHorizonalLayoutConfig").methodFinder()
            .filterByName("getTaskViewCenterYInWindowFraction")
            .first()
            .hookBeforeMethod {
                it.result = taskViewHeightValue
            }

    }
}
