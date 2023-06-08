package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class FreeFormCount : BaseHook() {
    override fun init() {
        // GetMaxMiuiFreeFormStackCount
        loadClass("com.android.server.wm.MiuiFreeFormStackDisplayStrategy").methodFinder().first {
            name == "getMaxMiuiFreeFormStackCount"
        }.createHook {
            returnConstant(256)
        }

        // GetMaxMiuiFreeFormStackCountForFlashBack
        loadClass("com.android.server.wm.MiuiFreeFormStackDisplayStrategy").methodFinder().first {
            name == "getMaxMiuiFreeFormStackCountForFlashBack"
        }.createHook {
            returnConstant(256)
        }

        // ShouldStopStartFreeform
        loadClass("com.android.server.wm.MiuiFreeFormManagerService").methodFinder().first {
            name == "shouldStopStartFreeform"
        }.createHook {
            returnConstant(false)
        }
    }
}