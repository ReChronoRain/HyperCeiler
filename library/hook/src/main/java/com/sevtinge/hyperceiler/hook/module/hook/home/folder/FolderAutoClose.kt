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
package com.sevtinge.hyperceiler.hook.module.hook.home.folder

import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod

object FolderAutoClose : BaseHook() {
    override fun init() {
        "com.miui.home.launcher.Launcher".hookAfterMethod(
            "launch", "com.miui.home.launcher.ShortcutInfo", View::class.java
        ) {
            val mHasLaunchedAppFromFolder = it.thisObject.getBooleanField("mHasLaunchedAppFromFolder")
            if (mHasLaunchedAppFromFolder) it.thisObject.callMethod("closeFolder")
        }
    }
}
