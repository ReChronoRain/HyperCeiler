package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.setObjectField

object HideLockscreenZenMode : BaseHook() {
    override fun init() {
        // hyperOS fix by hyper helper
        if (isMoreHyperOSVersion(1f)) {
            loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader)
                .methodFinder().first {
                    name == "updateVisibility"
                }.createHook {
                    before {
                        it.thisObject.setObjectField("manuallyDismissed", true)
                    }
                }
        } else {
            loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader)
                .methodFinder().first {
                    name == "shouldBeVisible"
                }.createHook {
                    returnConstant(false)
                }
        }
    }
}
