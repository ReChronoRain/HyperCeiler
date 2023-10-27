package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object RedirectToNotificationChannelSetting : BaseHook() {
    override fun init() {
        var statusBarNotification: StatusBarNotification? = null
        loadClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow")
            .methodFinder()
            .filterByName("onClickInfoItem").first().createHook {
                before { param ->
                    param.thisObject.objectHelper {
                        EzXHelper.initAppContext(getObjectOrNullAs<Context>("mContext"))
                        statusBarNotification = getObjectOrNullAs<StatusBarNotification>("mSbn")
                    }
                }
                after {
                    statusBarNotification = null
                }
            }
        loadClass("com.android.systemui.statusbar.notification.NotificationSettingsHelper")
            .methodFinder()
            .filterByName("startAppNotificationSettings").first().createHook {
                before { param ->
                    val intent = Intent(Intent.ACTION_MAIN)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setClassName("com.android.settings", "com.android.settings.SubSettings")
                        .putExtra(
                            ":android:show_fragment",
                            "com.android.settings.notification.ChannelNotificationSettings"
                        )
                        .putExtra(Settings.EXTRA_APP_PACKAGE, statusBarNotification!!.packageName)
                        .putExtra(
                            Settings.EXTRA_CHANNEL_ID,
                            statusBarNotification!!.notification.channelId
                        )
                        .putExtra("app_uid", statusBarNotification!!.uid)
                        .putExtra(
                            Settings.EXTRA_CONVERSATION_ID,
                            statusBarNotification!!.notification.shortcutId
                        )
                    val userHandleCurrent = getStaticObjectOrNullAs<UserHandle>(
                        UserHandle::class.java,
                        "CURRENT"
                    )
                    invokeMethodBestMatch(
                        appContext,
                        "startActivityAsUser",
                        null,
                        intent,
                        userHandleCurrent
                    )
                    param.result = null
                }
            }
    }
}
