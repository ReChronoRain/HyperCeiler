package com.sevtinge.cemiuiler.module.various

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object NoAccessDeviceLogsRequest : BaseHook() {
    override fun init() {
        Helpers.hookAllMethods(
            "com.android.server.logcat.LogcatManagerService",
            lpparam.classLoader,
            "onLogAccessRequested",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])
                    param.result = null
                }
            })
    }
}