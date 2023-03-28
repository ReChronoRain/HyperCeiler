package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.putObject
import com.sevtinge.cemiuiler.module.base.BaseHook

class NotificationIconColumns : BaseHook() {
    override fun init() {
        val maxIconsNum = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 3)
        val maxDotsNum = mPrefsMap.getInt("system_ui_status_bar_notification_dots_maximum", 3)
        findMethod("com.android.systemui.statusbar.phone.NotificationIconContainer") {
            name == "miuiShowNotificationIcons" && parameterCount == 1
        }.hookReplace {
            if (it.args[0] as Boolean) {
                it.thisObject.putObject("MAX_DOTS", maxDotsNum)
                it.thisObject.putObject("MAX_STATIC_ICONS", maxIconsNum)
                it.thisObject.putObject("MAX_VISIBLE_ICONS_ON_LOCK", maxIconsNum)
            } else {
                it.thisObject.putObject("MAX_DOTS", 0)
                it.thisObject.putObject("MAX_STATIC_ICONS", 0)
                it.thisObject.putObject("MAX_VISIBLE_ICONS_ON_LOCK", 0)
            }
            it.thisObject.invokeMethod("updateState")
        }
    }
}