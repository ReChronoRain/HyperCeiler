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

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge

object CardTextSize : BaseHook() {
    override fun init() {
        val recentTextSize = PrefsBridge.getInt("home_recent_text_size", -1)
        val taskViewHeaderClass = findClass("com.miui.home.recents.views.TaskViewHeader")
        taskViewHeaderClass.afterHookMethod(
            "onFinishInflate"
        ) {
            val mTitle = it.thisObject.getObjectField("mTitleView") as TextView
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, recentTextSize.toFloat())
            if (recentTextSize == 0) mTitle.visibility = View.GONE
        }
    }
}
