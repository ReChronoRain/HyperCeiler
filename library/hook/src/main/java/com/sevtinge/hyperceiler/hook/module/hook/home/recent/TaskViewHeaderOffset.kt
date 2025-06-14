package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.setIntField

object TaskViewHeaderOffset : BaseHook() {
    override fun init() {
        val horizontalOffsetValue by lazy {
            mPrefsMap.getInt("task_view_header_horizontal_offset", 30)
        }

        loadClass("com.miui.home.recents.views.TaskViewHeader")
            .methodFinder()
            .filterByName("onAttachedToWindow")
            .first()
            .hookAfterMethod {
                val thisObject = it.thisObject
                thisObject.setIntField("mHeaderButtonPadding", horizontalOffsetValue)
                thisObject.callMethod("setPadding", horizontalOffsetValue, 0, horizontalOffsetValue, 0)
            }

    }
}
