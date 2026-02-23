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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge

object HideRecentCard : BaseHook() {
    override fun init() {
        findClass("com.android.systemui.shared.recents.system.ActivityManagerWrapper")
            .afterHookMethod(
                "needRemoveTask",
                "com.android.systemui.shared.recents.model.GroupedRecentTaskInfoCompat"
            ) { param ->
                val pkgName = param.args[0]
                    ?.getObjectField("mMainTaskInfo")
                    ?.getObjectField("realActivity")
                    ?.callMethod("getPackageName")
                val selectedApps = PrefsBridge.getStringSet("home_recent_hide_card")
                if (selectedApps.contains(pkgName)) {
                    param.result = true
                }
            }
    }
}
