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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getBooleanFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks


object AutoDismissExpandedPopupsHook : BaseHook() {

    private val showTime by lazy {
        mPrefsMap.getInt("system_ui_control_center_expand_notification_show_time", 45) * 100L
    }
    private val mHeadsUpEntryPhoneClass by lazy {
        if (isMoreAndroidVersion(36)) {
            // Android 16 合并了 HeadsUpManagerPhone，不再继承
            loadClass($$"com.android.systemui.statusbar.notification.headsup.HeadsUpManagerImpl$HeadsUpEntry")
        } else {
            loadClass($$"com.android.systemui.statusbar.phone.HeadsUpManagerPhone$HeadsUpEntryPhone")
        }
    }

    override fun init() {
        mHeadsUpEntryPhoneClass.methodFinder()
            .filterByName("updateEntry")
            .first().createAfterHook {
                val headsUpEntry = it.thisObject ?: return@createAfterHook
                val expanded =
                    headsUpEntry.getBooleanFieldOrNull("mExpanded") ?:
                    headsUpEntry.getBooleanFieldOrNull("expanded") ?: return@createAfterHook
                val remoteInputActive =
                    headsUpEntry.getBooleanFieldOrNull("mRemoteInputActive") ?:
                    headsUpEntry.getBooleanFieldOrNull("remoteInputActive") ?: return@createAfterHook
                val mEntry = headsUpEntry.getObjectField("mEntry") ?: return@createAfterHook
                val rowPinned = mEntry.callMethod("isRowPinned") as Boolean

                if (expanded && rowPinned && !remoteInputActive) {
                    val mRemoveAlertRunnable = (
                            headsUpEntry.getObjectFieldOrNull("mRemoveRunnable") ?:
                            headsUpEntry.getObjectFieldOrNull("mRemoveAlertRunnable")
                        ) as Runnable
                    val extended = headsUpEntry.getBooleanField("extended")

                    // Android 15 开始没有 Handler
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        mRemoveAlertRunnable, if (extended) 10000 else showTime
                    )

                }
            }

        loadClass("com.android.systemui.statusbar.phone.StatusBarNotificationPresenter")
            .methodFinder()
            .filterByName("onExpandClicked")
            .toList().createHooks {
                after {
                    val expanded = it.args[1] as Boolean
                    val mKeyguardStateController = it.thisObject.getObjectField("mKeyguardStateController") ?: return@after
                    val mShowing =
                        mKeyguardStateController.getBooleanField("mShowing")

                    if (expanded && !mShowing) {
                        val headsUpManagerPhone =
                            it.thisObject.getObjectField("mHeadsUpManager") ?: return@after
                        val headsUpEntry = headsUpManagerPhone.callMethod(
                            "getHeadsUpEntry",
                            it.args[0].getObjectField("mKey")
                        )

                        if (headsUpEntry != null) {
                            val isRowPinned = it.args[0].callMethod("isRowPinned") as Boolean
                            if (isRowPinned) {
                                val mRemoveAlertRunnable = (
                                    headsUpEntry.getObjectFieldOrNull("mRemoveRunnable") ?:
                                    headsUpEntry.getObjectFieldOrNull("mRemoveAlertRunnable")
                                ) as Runnable

                                // Android 15 开始没有 Handler
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                    mRemoveAlertRunnable, showTime
                                )
                            }
                        }
                    }
                }
            }
    }
}
