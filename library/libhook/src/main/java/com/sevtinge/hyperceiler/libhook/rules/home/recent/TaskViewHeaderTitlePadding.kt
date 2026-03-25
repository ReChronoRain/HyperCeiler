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
package com.sevtinge.hyperceiler.libhook.rules.home.recent

import android.view.View
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField

/**
 * Aligns Recents header title/lock views with the header button padding.
 */
object TaskViewHeaderTitlePadding : BaseHook() {
    private const val HOME_PKG = "com.miui.home"
    private const val RES_PADDING = "recents_task_view_header_button_padding"
    private const val RES_TITLE_ID = "title"
    private const val RES_LOCK_ID = "lock_imageView"

    override fun init() {
        val taskViewHeaderClass = findClass("com.miui.home.recents.views.TaskViewHeader")
        taskViewHeaderClass.afterHookMethod("onFinishInflate") { param ->
            val hostView = param.thisObject as? View
            if (hostView != null) {
                val res = hostView.resources
                val paddingResId = res.getIdentifier(RES_PADDING, "dimen", HOME_PKG)
                if (paddingResId != 0) {
                    val paddingBottom = res.getDimensionPixelSize(paddingResId)
                    val titleView = try {
                        param.thisObject.getObjectField("mTitleView") as? TextView
                    } catch (_: Throwable) {
                        null
                    } ?: run {
                        val titleId = res.getIdentifier(RES_TITLE_ID, "id", HOME_PKG)
                        if (titleId == 0) null else hostView.findViewById(titleId)
                    }

                    titleView?.setPaddingRelative(
                        paddingBottom,
                        titleView.paddingTop,
                        titleView.paddingEnd,
                        paddingBottom
                    )

                    val lockId = res.getIdentifier(RES_LOCK_ID, "id", HOME_PKG)
                    if (lockId != 0) {
                        val lockView = hostView.findViewById<View>(lockId)
                        lockView?.setPadding(
                            lockView.paddingLeft,
                            lockView.paddingTop,
                            lockView.paddingRight,
                            paddingBottom
                        )
                    }
                }
            }
        }
    }
}
