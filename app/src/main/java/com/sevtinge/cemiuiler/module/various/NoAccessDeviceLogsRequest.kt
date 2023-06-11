package com.sevtinge.cemiuiler.module.various

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers


object NoAccessDeviceLogsRequest : BaseHook() {
    override fun init() {
        try {
            loadClass("com.android.server.logcat.LogcatManagerService").methodFinder().filter {
                name == "onLogAccessRequested"
            }.toList().createHooks {
                before { param ->
                    XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])
                    param.result = null
                }
            }
        } catch (t: Throwable) {
            logE(t)
        }
    }
}
