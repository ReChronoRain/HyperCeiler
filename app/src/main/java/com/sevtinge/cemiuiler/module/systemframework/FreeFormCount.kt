package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

class FreeFormCount : BaseHook() {
    override fun init() {
        // GetMaxMiuiFreeFormStackCount
        findMethod("com.android.server.wm.MiuiFreeFormStackDisplayStrategy") {
            name == "getMaxMiuiFreeFormStackCount"
        }.hookReturnConstant(256)
        // GetMaxMiuiFreeFormStackCountForFlashBack
        findMethod("com.android.server.wm.MiuiFreeFormStackDisplayStrategy") {
            name == "getMaxMiuiFreeFormStackCountForFlashBack"
        }.hookReturnConstant(256)
        // ShouldStopStartFreeform
        findMethod("com.android.server.wm.MiuiFreeFormManagerService") {
            name == "shouldStopStartFreeform"
        }.hookReturnConstant(false)
    }
}