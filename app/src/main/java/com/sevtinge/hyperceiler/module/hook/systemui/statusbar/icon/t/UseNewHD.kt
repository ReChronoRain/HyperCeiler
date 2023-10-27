package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.t

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object UseNewHD : BaseHook() {
    // 仅供 Android 13 设备使用，部分未进版机型依旧不可用
    override fun init() {
        runCatching {
            loadClass("com.android.systemui.statusbar.policy.HDController").methodFinder().first {
                name == "isVisible"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
