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
package com.sevtinge.hyperceiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*

object ShowMoreFpsList : BaseHook() {
    override fun init() {
        loadClass("miui.util.FeatureParser").methodFinder()
            .filterByName("getIntArray")
            .single().createHook {
                before {
                    if (it.args[0] == "fpsList") {
                        if (mPrefsMap.getBoolean("mi_settings_show_fps") && mPrefsMap.getBoolean("bamboo_misettings_fps_list_add_1hz")) {
                            it.result = intArrayOf(144, 120, 90, 60, 30, 1)
                        } else if (mPrefsMap.getBoolean("mi_settings_show_fps")) {
                            it.result = intArrayOf(144, 120, 90, 60, 30)
                        } else if (mPrefsMap.getBoolean("bamboo_misettings_fps_list_add_1hz")) {
                            it.result = intArrayOf(120, 90, 60, 1)
                        }
                    }
                }
            }
    }
}
