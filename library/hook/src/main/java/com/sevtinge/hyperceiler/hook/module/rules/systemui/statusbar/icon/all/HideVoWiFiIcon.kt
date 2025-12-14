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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setBooleanField
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object HideVoWiFiIcon : BaseHook() {
    private val hideVoWifi by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi")
    }
    private val hideVolte by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_icon_volte")
    }

    override fun init() {
        loadClass($$"com.miui.interfaces.IOperatorCustomizedPolicy$OperatorConfig").constructors[0].createHook {
            after {
                it.thisObject.setBooleanField("hideVowifi", hideVoWifi)
                it.thisObject.setBooleanField("hideVolte", hideVolte)
            }
        }
    }
}
