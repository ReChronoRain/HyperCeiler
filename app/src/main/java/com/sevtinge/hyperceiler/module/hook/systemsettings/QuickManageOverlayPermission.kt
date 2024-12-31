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
package com.sevtinge.hyperceiler.module.hook.systemsettings

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.sevtinge.hyperceiler.module.base.BaseHook

class QuickManageOverlayPermission : BaseHook() {
    override fun init() {
        findAndHookMethod("com.android.settings.SettingsActivity",
            "redirectTabletActivity",
            Bundle::class.java,
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val intent = (param.thisObject as Activity).intent
                    if (intent.action != Settings.ACTION_MANAGE_OVERLAY_PERMISSION || intent.data == null || intent.data!!.scheme != "package") return
                    param.thisObject.objectHelper().setObjectUntilSuperclass(
                        "initialFragmentName",
                        "com.android.settings.applications.appinfo.DrawOverlayDetails"
                    )
                }
            })
    }
}
