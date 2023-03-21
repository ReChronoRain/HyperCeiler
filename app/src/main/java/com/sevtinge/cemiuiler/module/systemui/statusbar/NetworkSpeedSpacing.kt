package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook


object NetworkSpeedSpacing : BaseHook() {
//  网速更新间隔
    override fun init() {
        findMethod("com.android.systemui.statusbar.policy.NetworkSpeedController") {
            name == "postUpdateNetworkSpeedDelay" && parameterTypes[0] == Long::class.java
        }.hookBefore {
            val originInterval = it.args[0] as Long
            if (originInterval == 4000L) {
                val newInterval = mPrefsMap.getInt("status_bar_network_speed_refresh_speed", 4) * 1000L
                it.args[0] = newInterval
            }
        }
    }
}