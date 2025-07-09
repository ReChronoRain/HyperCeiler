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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.ImageView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.helper.ObjectHelper.`-Static`.objectHelper
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.invokeStaticMethodBestMatch
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ObjectUtil.invokeMethodBestMatch
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.initAppContext
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object RedirectToNotificationChannelSetting : BaseHook() {
    // by starVoyager
    private var statusBarNotification: StatusBarNotification? = null
    private val clazzMiuiNotificationMenuRow by lazy {
        loadClass(
            "com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow",
            lpparam.classLoader
        )
    }

    override fun init() {
        // hyperos fix by yife
        val clazzDependency = loadClass("com.android.systemui.Dependency")
        val clazzModalController =
            loadClass("com.android.systemui.statusbar.notification.modal.ModalController")
        val clazzCommandQueue = loadClass("com.android.systemui.statusbar.CommandQueue")
        clazzMiuiNotificationMenuRow.methodFinder().filterByName("createMenuViews").first()
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
                        val modalController = invokeStaticMethodBestMatch(
                            clazzDependency, "get", null, clazzModalController
                        ) ?: return@setOnClickListener
                        invokeMethodBestMatch(
                            modalController, "animExitModal", null, 50L, true, "MORE", false
                        )
                        val commandQueue = invokeStaticMethodBestMatch(
                            clazzDependency, "get", null, clazzCommandQueue
                        ) ?: return@setOnClickListener
                        invokeMethodBestMatch(
                            commandQueue, "animateCollapsePanels", null, 0, false
                        )
                    }
                }
            }
        clazzMiuiNotificationMenuRow.methodFinder().filterByName("onClickInfoItem").firstOrNull()
            ?.createHook {
                before { param ->
                    param.thisObject.objectHelper {
                        initAppContext(getObjectFieldOrNullAs<Context>("mContext"))
                        statusBarNotification = getObjectFieldOrNullAs("mSbn") as StatusBarNotification?
                    }
                }
                after {
                    statusBarNotification = null
                }
            }
        loadClass("com.android.systemui.statusbar.notification.NotificationSettingsHelper").methodFinder()
            .filterByName("startAppNotificationSettings").firstOrNull()?.createHook {
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
        val userHandleCurrent =
            UserHandle::class.java.getObjectFieldOrNullAs<UserHandle>("CURRENT")
        invokeMethodBestMatch(
            appContext, "startActivityAsUser", null, intent, userHandleCurrent
        )
    }
}
