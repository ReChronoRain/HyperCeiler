/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.ImageView
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Dependency
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.appContext
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.initAppContext
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object RedirectToNotificationChannelSetting : BaseHook() {
    // by starVoyager
    private var statusBarNotification: StatusBarNotification? = null
    private val clazzMiuiNotificationMenuRow by lazy {
        findClass(
            "com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow",
            lpparam.classLoader
        )
    }

    override fun init() {
        // hyperos fix by yife
        val clazzModalController = if (isMoreAndroidVersion(36)) {
            findClass("com.android.systemui.statusbar.notification.modal.IModalController")
        } else {
            findClass("com.android.systemui.statusbar.notification.modal.ModalController")
        }
        val clazzCommandQueue = findClass("com.android.systemui.statusbar.CommandQueue")
        clazzMiuiNotificationMenuRow.findMethod { name("createMenuViews") }
            .createHook {
                after { param ->
                    val mSbn =
                        param.thisObject.getObjectFieldOrNullAs<StatusBarNotification>("mSbn") ?: return@after
                    val mInfoItem =
                        param.thisObject.getObjectFieldOrNull("mInfoItem") ?: return@after
                    initAppContext(param.thisObject.getObjectFieldOrNullAs<Context>("mContext"))
                    val mIcon = mInfoItem.getObjectFieldOrNullAs<ImageView>("mIcon") ?: return@after
                    mIcon.setOnClickListener {
                        startChannelNotificationSettings(mSbn)
                        runCatching {
                            if (!isMoreAndroidVersion(36)) {
                                val modalController =
                                    Dependency.getDependencyInner(clazzModalController)
                                        ?: return@setOnClickListener
                                callMethod(
                                    modalController, "animExitModal", 50L, true, "MORE", false
                                )
                            }
                            val commandQueue =
                                Dependency.getDependencyInner(clazzCommandQueue)
                                    ?: return@setOnClickListener
                            callMethod(
                                commandQueue, "animateCollapsePanels",  0, false
                            )
                        }.onFailure {
                            XposedLog.w(TAG, lpparam.packageName, "RedirectToNotificationChannelSetting: ", it)
                        }

                    }
                }
            }
        clazzMiuiNotificationMenuRow.findMethodOrNull { name("onClickInfoItem") }
            ?.createHook {
                before { param ->
                    initAppContext(param.thisObject.getObjectFieldOrNullAs<Context>("mContext"))
                    statusBarNotification =
                        param.thisObject.getObjectFieldOrNullAs<StatusBarNotification>("mSbn")
                }
                after {
                    statusBarNotification = null
                }
            }


        val notificationSettingsHelper =
            findClassIfExists("com.miui.systemui.notification.NotificationSettingsHelper")
                ?: findClass("com.android.systemui.statusbar.notification.NotificationSettingsHelper")
        notificationSettingsHelper.findMethodOrNull { name("startAppNotificationSettings") }?.createHook {
                before { param ->
                    startChannelNotificationSettings(statusBarNotification!!)
                    param.result = null
                }
            }
    }

    private fun startChannelNotificationSettings(statusBarNotification: StatusBarNotification) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            setClassName(
                "com.android.settings",
                "com.android.settings.SubSettings"
            )

            putExtra(
                ":android:show_fragment",
                "com.android.settings.notification.ChannelNotificationSettings"
            )
            putExtra(
                Settings.EXTRA_APP_PACKAGE,
                statusBarNotification.packageName
            )
            putExtra(
                Settings.EXTRA_CHANNEL_ID,
                statusBarNotification.notification.channelId
            )
            putExtra("app_uid", statusBarNotification.uid)
                .putExtra(
                Settings.EXTRA_CONVERSATION_ID, statusBarNotification.notification.shortcutId
            )
        }
        appContext.startActivity(intent)
        /*val userHandleCurrent =
            UserHandle::class.java.getObjectFieldOrNullAs<UserHandle>("CURRENT")

        invokeMethodBestMatch(
            appContext, "startActivityAsUser", null, intent, userHandleCurrent
        )*/
    }
}
