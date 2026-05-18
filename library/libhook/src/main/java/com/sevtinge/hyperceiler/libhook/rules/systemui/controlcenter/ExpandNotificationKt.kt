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

import android.view.View
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object ExpandNotificationKt : BaseHook() {
    private val mPkg by lazy {
        PrefsBridge.getStringSet("system_ui_control_center_expand_notification")
    }
    private val mExpandNotificationRowClass by lazy {
        loadClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")
    }

    override fun init() {
        mExpandNotificationRowClass.findMethod { name("setFeedbackIcon") }.createBeforeHook {
                    val mOnKeyguard =
                        it.thisObject.getObjectField("mOnKeyguard") as Boolean
                    if (!mOnKeyguard) {
                        val notification =
                            it.thisObject.callMethod("getEntry")!!.getObjectField("mSbn")
                        val pkgName =
                            notification!!.callMethod("getPackageName") as String?
                        if (mPkg.contains(pkgName)) it.thisObject.callMethod(
                            "setSystemExpanded",
                            true
                        )
                    }
                }

        mExpandNotificationRowClass.findMethod { name("setHeadsUp") }.createAfterHook {
                    val mOnKeyguard =
                        it.thisObject.getObjectField("mOnKeyguard") as Boolean
                    val showHeadsUp = it.args[0] as Boolean
                    if (!mOnKeyguard && showHeadsUp) {
                        val notifyRow = it.thisObject as View
                        val notification =
                            it.thisObject.callMethod("getEntry")!!.getObjectField("mSbn")
                        val pkgName =
                            notification!!.callMethod("getPackageName") as String?
                        if (mPkg.contains(pkgName)) {
                            val expandNotify = Runnable {
                                val mExpandClickListener =
                                    it.thisObject.getObjectField("mExpandClickListener") as View.OnClickListener
                                mExpandClickListener.onClick(notifyRow)
                            }
                            notifyRow.postDelayed(expandNotify, 60)
                        }
                    }
                }
    }
}
