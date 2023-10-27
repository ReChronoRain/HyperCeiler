package com.sevtinge.hyperceiler.module.hook.home.dock

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object DisableRecentsIcon : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.launcher.hotseats.HotSeatsListRecentsAppProvider").methodFinder().first {
            name == "updateFinalRecommendTasks"
        }.createHook {
            before { param ->
                param.result = true
            }
        }
    }
}
