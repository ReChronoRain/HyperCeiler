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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import com.sevtinge.hyperceiler.utils.setBooleanField

object HideVoWiFiIcon : BaseHook() {
    override fun init() {
        val hideVoWifi by lazy {
            mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi")
        }
        val hideVolte by lazy {
            mPrefsMap.getBoolean("system_ui_status_bar_icon_volte")
        }
        if (isAndroidVersion(34)) {
            loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig").constructors[0].createHook {
                after {
                    it.thisObject.setBooleanField("hideVowifi", hideVoWifi)
                    it.thisObject.setBooleanField("hideVolte", hideVolte)
                }
            }
        } else if (hideVoWifi) {
            loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig").methodFinder().first {
                name == "getHideVowifi"
            }.createHook { returnConstant(true) }
        }
    }
}
