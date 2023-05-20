package com.sevtinge.cemiuiler.module.various

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.findClass
import de.robv.android.xposed.XposedHelpers


object NoAccessDeviceLogsRequest : BaseHook() {
    override fun init() {
        hookAllMethods(
            "com.android.server.logcat.LogcatManagerService".findClass(),
            "onLogAccessRequested",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])
                    param.result = null
                }
            })
    }
}