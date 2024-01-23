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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.recent

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

object CardTextSize : BaseHook() {
    override fun init() {
        val recentTextSize = mPrefsMap.getInt("home_recent_text_size", -1)
        if (recentTextSize != -1) {
            val taskViewHeaderClass = "com.miui.home.recents.views.TaskViewHeader".findClass()
            taskViewHeaderClass.hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleView") as TextView
                mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, recentTextSize.toFloat())
                if (recentTextSize == 0) mTitle.visibility = View.GONE
            }
        }
    }
}
