package com.sevtinge.hyperceiler.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.IS_TABLET
import com.sevtinge.hyperceiler.utils.api.isPad

object AlwaysShowCleanUp: BaseHook() {
    override fun init() {
        loadClass(
            when (IS_TABLET) {
                false -> "com.miui.home.recents.views.RecentsContainer"
                true -> "com.miui.home.recents.views.RecentsDecorations"
            }
        ).methodFinder().filterByName("updateClearContainerVisible")
            .first().createHook {
                returnConstant(true)
            }
    }
}
