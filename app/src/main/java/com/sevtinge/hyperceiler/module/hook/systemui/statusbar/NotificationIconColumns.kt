package com.sevtinge.hyperceiler.module.hook.systemui.statusbar

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidR
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidT
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidU
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getBooleanField
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterAllConstructors
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import com.sevtinge.hyperceiler.utils.setObjectField

object NotificationIconColumns : BaseHook() {
    override fun init() {
        if (isAndroidR()) return

        val maxIconsNum = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 3)
        val maxDotsNum = mPrefsMap.getInt("system_ui_status_bar_notification_dots_maximum", 3)

        if (isAndroidU() && isMoreHyperOSVersion(1f)) {
            mAndroidU(maxIconsNum, maxDotsNum)
        } else {
            mAndroidS(maxIconsNum, maxDotsNum)
        }
    }

    private fun mAndroidU(maxIconsNum: Int, maxDotsNum: Int) {
        val notificationIconObserver = findClass("com.android.systemui.statusbar.policy.NotificationIconObserver")
        var getBoolean = false

        notificationIconObserver.hookAfterAllConstructors {
            getBoolean = it.thisObject.getBooleanField("mShowNotificationIcons")
            XposedLogUtils.logI(getBoolean.toString())
        }

        loadClass("com.android.systemui.statusbar.phone.NotificationIconAreaController").methodFinder()
            .filterByName("init")
            .first().createHook {
                after {
                    val getIcons = it.thisObject.getObjectField("mNotificationIcons")
                    if (getBoolean) {
                        getIcons.setObjectField("mMaxDot", maxDotsNum)
                        getIcons.setObjectField("mMaxStaticIcon", maxIconsNum)
                        getIcons.setObjectField("mMaxIconsOnLockscreen", maxIconsNum)
                    } else {
                        getIcons.setObjectField("mMaxDot", 0)
                        getIcons.setObjectField("mMaxStaticIcon", 0)
                        getIcons.setObjectField("mMaxIconsOnLockscreen", 0)
                    }
                    getIcons?.callMethod("resetViewStates")
                    getIcons?.callMethod("calculateIconXTranslations")
                    getIcons?.callMethod("applyIconStates")
                    XposedLogUtils.logI("NotificationIconAreaController hook success")
                }
            }
    }

    private fun mAndroidS(maxIconsNum: Int, maxDotsNum: Int) {
        loadClass("com.android.systemui.statusbar.phone.NotificationIconContainer").methodFinder()
            .filterByName("miuiShowNotificationIcons")
            .filterByParamCount(1)
            .first().createHook {
                replace {
                    if (it.args[0] as Boolean) {
                        it.thisObject.setObjectField("MAX_DOTS", maxDotsNum)
                        it.thisObject.setObjectField("MAX_STATIC_ICONS", maxIconsNum)
                        if (isAndroidT()) {
                            it.thisObject.setObjectField("MAX_ICONS_ON_LOCKSCREEN", maxIconsNum)
                        } else {
                            it.thisObject.setObjectField("MAX_VISIBLE_ICONS_ON_LOCK", maxIconsNum)
                        }
                    } else {
                        it.thisObject.setObjectField("MAX_DOTS", 0)
                        it.thisObject.setObjectField("MAX_STATIC_ICONS", 0)
                        if (isAndroidT()) {
                            it.thisObject.setObjectField("MAX_ICONS_ON_LOCKSCREEN", 0)
                        } else {
                            it.thisObject.setObjectField("MAX_VISIBLE_ICONS_ON_LOCK", 0)
                        }
                    }
                    it.thisObject.callMethod("updateState")
                }
            }
    }
}
