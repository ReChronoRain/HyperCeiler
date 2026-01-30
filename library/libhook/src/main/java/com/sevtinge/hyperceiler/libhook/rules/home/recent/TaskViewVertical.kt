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

import android.graphics.RectF
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object TaskViewVertical : BaseHook() {
    override fun init() {

        val value = mPrefsMap.getInt("home_recent_vertical_task_view_card_size", 100).toFloat() / 100
        if (value == -1f || value == 1f) return
        findClass("com.miui.home.recents.views.TaskStackViewsAlgorithmVertical").replaceMethod(
            "scaleTaskView", RectF::class.java
        ) {
            findClass("com.miui.home.recents.util.Utilities").callStaticMethod(
                "scaleRectAboutCenter",
                it.args[0],
                value * findClass("com.miui.home.recents.util.Utilities")
                    .callStaticMethod("getTaskViewScale", appContext) as Float
            )
        }

    }
}
