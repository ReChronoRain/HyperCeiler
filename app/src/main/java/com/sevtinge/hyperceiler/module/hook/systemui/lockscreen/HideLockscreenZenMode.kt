package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object HideLockscreenZenMode : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader)
            .methodFinder().first {
                name == "shouldBeVisible"
            }.createHook {
                returnConstant(false)
            }
    }
}
