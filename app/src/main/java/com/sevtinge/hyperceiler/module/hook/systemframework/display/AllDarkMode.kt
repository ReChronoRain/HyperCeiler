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
package com.sevtinge.hyperceiler.module.hook.systemframework.display

import android.content.pm.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*

// from SetoHook by SetoSkins
class AllDarkMode : BaseHook() {
    override fun init() {
        if (isInternational()) return
        val clazzForceDarkAppListManager =
            loadClass("com.android.server.ForceDarkAppListManager")
        clazzForceDarkAppListManager.methodFinder().filterByName("getDarkModeAppList").toList()
            .createHooks {
                before {
                    val originalValue = XposedHelpers.getStaticBooleanField(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD")
                    setStaticObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", true)
                    it.setObjectExtra("originalValue", originalValue)
                }
                after {
                    val originalValue = it.getObjectExtra("originalValue")
                    setStaticObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", originalValue)
                }
            }
        clazzForceDarkAppListManager.methodFinder().filterByName("shouldShowInSettings").toList()
            .createHooks {
                before { param ->
                    val info = param.args[0] as ApplicationInfo?
                    param.result =
                        !(info == null || (invokeMethodBestMatch(
                            info,
                            "isSystemApp"
                        ) as Boolean) || info.uid < 10000)
                }
            }
    }
}
