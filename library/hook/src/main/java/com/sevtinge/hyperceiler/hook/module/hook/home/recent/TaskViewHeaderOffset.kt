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
package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.setIntField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass

object TaskViewHeaderOffset : BaseHook() {
    override fun init() {
        val horizontalOffsetValue by lazy {
            mPrefsMap.getInt("task_view_header_horizontal_offset", 30)
        }

        loadClass("com.miui.home.recents.views.TaskViewHeader")
            .methodFinder()
            .filterByName("onAttachedToWindow")
            .first()
            .hookAfterMethod {
                val thisObject = it.thisObject
                thisObject.setIntField("mHeaderButtonPadding", horizontalOffsetValue)
                thisObject.callMethod("setPadding", horizontalOffsetValue, 0, horizontalOffsetValue, 0)
            }

    }
}
