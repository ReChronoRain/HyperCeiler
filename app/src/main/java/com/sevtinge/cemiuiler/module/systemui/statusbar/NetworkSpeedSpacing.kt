package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers

object NetworkSpeedSpacing : BaseHook() {
    override fun init() {
//      网速更新间隔
        Helpers.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController",
            lpparam.classLoader,
            "postUpdateNetworkSpeedDelay",
            Long::class.javaPrimitiveType,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val originInterval = param.args[0] as Long
                    if (originInterval == 4000L) {
                        val newInterval = mPrefsMap.getInt(
                            "system_ui_statusbar_network_speed_update_spacing",
                            4
                        ) * 1000L
                        param.args[0] = newInterval
                    }
                }
            })

    }
}