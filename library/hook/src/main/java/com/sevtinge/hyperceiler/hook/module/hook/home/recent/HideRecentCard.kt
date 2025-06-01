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
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField

object HideRecentCard : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.android.systemui.shared.recents.system.ActivityManagerWrapper",
            "needRemoveTask",
            "com.android.systemui.shared.recents.model.GroupedRecentTaskInfoCompat",
            object : HookTool.MethodHook() {
                override fun after(param: MethodHookParam) {
                    val pkgName = param.args[0]
                        ?.getObjectField("mMainTaskInfo")
                        ?.getObjectField("realActivity")
                        ?.callMethod("getPackageName")
                    val selectedApps = mPrefsMap.getStringSet("home_recent_hide_card")
                    if (selectedApps.contains(pkgName)) {
                        param.result = true
                    }
                }
            })
    }
}
