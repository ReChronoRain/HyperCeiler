package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object DisableCleaner : BaseHook() {
    override fun init() {
        findMethod("com.android.server.am.ActivityManagerService") {
            name == "checkExcessivePowerUsage"
        }.hookBefore {
            it.result = null
        }
        findMethod("com.android.server.am.OomAdjuster") {
            name == "updateOomAdjInnerLSP"
        }.hookBefore {
            it.result = null
        }
        findMethod("com.android.server.am.PhantomProcessList") {
            name == "trimPhantomProcessesIfNecessary"
        }.hookBefore {
            it.result = null
        }
        findMethod("com.android.server.am.SystemPressureController") {
            name == "onSystemReady"
        }.hookBefore {
            it.result = null
        }
        findMethod("com.android.server.wm.RecentTasks") {
            name == "trimInactiveRecentTasks"
        }.hookBefore {
            it.result = null
        }
    }
}
