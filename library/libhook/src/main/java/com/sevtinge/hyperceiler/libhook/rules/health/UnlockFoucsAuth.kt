package com.sevtinge.hyperceiler.libhook.rules.health

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object UnlockFoucsAuth : BaseHook() {
    override fun init() {
        loadClass("com.xiaomi.fitness.notify.BaseNotifySyncService")
            .findMethod {
                name("handleNotificationPosted")
            }.createHook {
                before {
                    val sbn = it.args[0] as StatusBarNotification
                    val notification = sbn.notification
                    val channelId = sbn.notification.channelId

                    if (channelId == "channel_id_focusNotifLyrics") {
                        // 清除 ongoing flag
                        notification.flags =
                            notification.flags and Notification.FLAG_ONGOING_EVENT.inv()
                    }
                }
            }

        loadClass("com.xiaomi.fitness.notify.util.NotificationFilterHelper")
            .findMethod {
                name("isNotificationSpotlightAppInWhiteList")
            }.createHook {
                // 允许全部应用转发到手表
                returnConstant(true)
            }
    }
}
