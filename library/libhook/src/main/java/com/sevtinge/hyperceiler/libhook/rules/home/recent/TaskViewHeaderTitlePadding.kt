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

object TaskViewHeaderTitlePadding : BaseHook() {
    override fun init() {
        val taskViewHeaderClass = findClass("com.miui.home.recents.views.TaskViewHeader")
        taskViewHeaderClass.afterHookMethod("onFinishInflate") {
            val hostView = it.thisObject as? View ?: return@afterHookMethod
            val res = hostView.resources
            val paddingResId = res.getIdentifier(
                "recents_task_view_header_button_padding",
                "dimen",
                "com.miui.home"
            )
            if (paddingResId == 0) return@afterHookMethod

            val paddingBottom = res.getDimensionPixelSize(paddingResId)
            val titleView = runCatching {
                it.thisObject.getObjectField("mTitleView") as? TextView
            }.getOrNull() ?: run {
                val titleId = res.getIdentifier("title", "id", "com.miui.home")
                if (titleId == 0) null else hostView.findViewById(titleId)
            }

            titleView?.setPaddingRelative(
                paddingBottom,
                titleView.paddingTop,
                titleView.paddingEnd,
                paddingBottom
            )

            val lockId = res.getIdentifier("lock_imageView", "id", "com.miui.home")
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
