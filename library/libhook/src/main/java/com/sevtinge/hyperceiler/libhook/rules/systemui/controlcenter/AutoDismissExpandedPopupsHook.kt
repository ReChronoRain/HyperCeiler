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

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getBooleanField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getBooleanFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull


object AutoDismissExpandedPopupsHook : BaseHook() {

    private val showTime by lazy {
        PrefsBridge.getInt("system_ui_control_center_expand_notification_show_time", 45) * 100L
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
        mHeadsUpEntryPhoneClass.findMethod { name("updateEntry") }.createAfterHook {
                val headsUpEntry = it.thisObject
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

        loadClass("com.android.systemui.statusbar.phone.StatusBarNotificationPresenter").findAllMethods { name("onExpandClicked") }.createHooks {
                after {
                    dismissExpandedPopup(it)
                }
            }
    }

    private fun dismissExpandedPopup(param: HookParam) {
        // API 37 passes (row, EntryAdapter, expanded); older builds pass
        // (NotificationEntry, expanded). Do not cast the adapter to Boolean.
        if (param.args.size !in 2..3) return
        val expanded = param.args.lastOrNull() as? Boolean ?: return
        val entry = if (param.args.size == 3) {
            param.args[1]?.callMethodOrNull("getEntry")
        } else {
            param.args[0]
        } ?: return
        val key = if (param.args.size == 3) {
            param.args[1]?.callMethodOrNull("getKey")
        } else {
            entry.getObjectFieldOrNull("mKey")
        } as? String ?: return
        val mKeyguardStateController = param.thisObject.getObjectField("mKeyguardStateController") ?: return
        val mShowing =
            mKeyguardStateController.getBooleanField("mShowing")

        if (expanded && !mShowing) {
            val headsUpManagerPhone =
                param.thisObject.getObjectField("mHeadsUpManager") ?: return
            val headsUpEntry = headsUpManagerPhone.callMethod(
                "getHeadsUpEntry",
                key
            )

            if (headsUpEntry != null) {
                val isRowPinned = entry.callMethod("isRowPinned") as? Boolean ?: return
                val remoteInputActive = headsUpEntry.getBooleanFieldOrNull("mRemoteInputActive")
                    ?: headsUpEntry.getBooleanFieldOrNull("remoteInputActive") ?: false
                if (isRowPinned && !remoteInputActive) {
                    val mRemoveAlertRunnable = (
                        headsUpEntry.getObjectFieldOrNull("mRemoveRunnable") ?:
                        headsUpEntry.getObjectFieldOrNull("mRemoveAlertRunnable")
                    ) as? Runnable ?: return

                    // Android 15 开始没有 Handler
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        mRemoveAlertRunnable, showTime
                    )
                }
            }
        }
    }
}
