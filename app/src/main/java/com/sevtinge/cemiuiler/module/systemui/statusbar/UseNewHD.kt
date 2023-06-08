package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class UseNewHD : BaseHook() {
    // A13
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