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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getObjectField

object ExpandNotificationKt : BaseHook() {
    private val mPkg by lazy {
        mPrefsMap.getStringSet("system_ui_control_center_expand_notification")
    }
    private val mExpandNotificationRowClass by lazy {
        loadClass("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")
    }

    override fun init() {
        mExpandNotificationRowClass.methodFinder().filterByName("setFeedbackIcon")
            .first().createBeforeHook {
                val mOnKeyguard =
                    it.thisObject.getObjectField("mOnKeyguard") as Boolean
                if (!mOnKeyguard) {
                    val notification = it.thisObject.callMethod("getEntry")!!.getObjectField("mSbn")
                    val pkgName =
                        notification!!.callMethod("getPackageName") as String?
                    if (mPkg.contains(pkgName)) it.thisObject.callMethod("setSystemExpanded", true)
                }
            }

        mExpandNotificationRowClass.methodFinder().filterByName("setHeadsUp")
            .first().createAfterHook {
                val mOnKeyguard =
                    it.thisObject.getObjectField("mOnKeyguard") as Boolean
                val showHeadsUp = it.args[0] as Boolean
                if (!mOnKeyguard && showHeadsUp) {
                    val notifyRow = it.thisObject as View
                    val notification = it.thisObject.callMethod("getEntry")!!.getObjectField("mSbn")
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