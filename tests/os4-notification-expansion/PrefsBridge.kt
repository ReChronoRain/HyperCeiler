package com.sevtinge.hyperceiler.common.utils

object PrefsBridge {
    fun getInt(key: String, default: Int): Int {
        check(key == "system_ui_control_center_expand_notification_show_time")
        return 37
    }
}
