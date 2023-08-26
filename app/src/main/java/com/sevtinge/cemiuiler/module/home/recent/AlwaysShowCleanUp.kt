package com.sevtinge.cemiuiler.module.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.IS_TABLET
import com.sevtinge.cemiuiler.utils.api.isPad

class AlwaysShowCleanUp: BaseHook() {
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
