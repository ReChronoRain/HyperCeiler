package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.setObjectField

import com.sevtinge.cemiuiler.module.base.BaseHook

class NotificationIconColumns : BaseHook() {
    override fun init() {
        val maxIconsNum = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 3)
        val maxDotsNum = mPrefsMap.getInt("system_ui_status_bar_notification_dots_maximum", 3)
        loadClass("com.android.systemui.statusbar.phone.NotificationIconContainer").methodFinder().first {
            name == "miuiShowNotificationIcons" && parameterCount == 1
        }.createHook {
            replace {
                if (it.args[0] as Boolean) {
                    it.thisObject.setObjectField("MAX_DOTS", maxDotsNum)
                    it.thisObject.setObjectField("MAX_STATIC_ICONS", maxIconsNum)
                    it.thisObject.setObjectField("MAX_VISIBLE_ICONS_ON_LOCK", maxIconsNum)
                } else {
                    it.thisObject.setObjectField("MAX_DOTS", 0)
                    it.thisObject.setObjectField("MAX_STATIC_ICONS", 0)
                    it.thisObject.setObjectField("MAX_VISIBLE_ICONS_ON_LOCK", 0)
                }
                it.thisObject.callMethod("updateState")
            }
        }
    }
}