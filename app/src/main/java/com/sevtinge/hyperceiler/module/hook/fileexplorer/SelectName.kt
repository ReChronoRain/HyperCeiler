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
package com.sevtinge.hyperceiler.module.hook.fileexplorer

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectField

object SelectName : BaseHook() {
    override fun init() {
        loadClass("com.android.fileexplorer.view.FileListItem").methodFinder()
            .filterByName("onFinishInflate")
            .single().createHook {
            after {
                (it.thisObject.getObjectField("mFileNameTextView") as TextView).apply {
                    setTextIsSelectable(mPrefsMap.getBoolean("file_explorer_can_selectable"))
                    isSingleLine = mPrefsMap.getBoolean("file_explorer_is_single_line")
                }
            }
        }
    }
}
