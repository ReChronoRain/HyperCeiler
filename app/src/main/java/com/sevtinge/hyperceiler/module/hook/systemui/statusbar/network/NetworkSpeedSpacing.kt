package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network

import android.os.Build
import com.sevtinge.hyperceiler.module.base.BaseHook

object NetworkSpeedSpacing : BaseHook() {
    override fun init() {
        // 网速更新间隔
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android12+ 可用
            findAndHookMethod(
                "com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader,
                "postUpdateNetworkSpeedDelay",
                Long::class.javaPrimitiveType,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val originInterval = param.args[0] as Long
                        if (originInterval == 4000L) {
                            val newInterval =
                                mPrefsMap.getInt(
                                    "system_ui_statusbar_network_speed_update_spacing",
                                    4
                                ) * 1000L
                            param.args[0] = newInterval
                        }
                    }
                }
            )
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {  // Android11 可用
            findAndHookMethod(
                "com.android.systemui.statusbar.NetworkSpeedController", lpparam.classLoader,
                "postUpdateNetworkSpeedDelay",
                Long::class.javaPrimitiveType,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val originInterval = param.args[0] as Long
                        if (originInterval != 0L) {
                            val intervalTime =
                                mPrefsMap.getInt(
                                    "system_ui_statusbar_network_speed_update_spacing",
                                    4
                                ) * 1000L
                            param.args[0] = intervalTime
                        }
                    }
                }
            )
        }
    }
}
