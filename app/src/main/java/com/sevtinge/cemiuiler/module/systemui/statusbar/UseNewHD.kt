package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

class UseNewHD : BaseHook() {
    // A13
    override fun init() {
        runCatching {
            findMethod("com.android.systemui.statusbar.policy.HDController") {
                name == "isVisible"
            }.hookReturnConstant(true)
        }
    }
}