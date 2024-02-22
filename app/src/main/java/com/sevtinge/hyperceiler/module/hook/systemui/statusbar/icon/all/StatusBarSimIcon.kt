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

object StatusBarSimIcon : BaseHook() {
    private val card1 by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_card_1", 0) == 2
    }
    private val card2 by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_card_2", 0) == 2
    }

    override fun init() {
        loadClass("com.android.systemui.statusbar.phone.StatusBarSignalPolicy").methodFinder()
            .filterByName("hasCorrectSubs")
            .filterByParamTypes {
                it[0] == MutableList::class.java
            }.single().createHook {
                before {
                    val list = it.args[0] as MutableList<*>
                    /* val size = list.size*/
                    if (card2) {
                        list.removeAt(1)
                    }
                    if (card1) {
                        list.removeAt(0)
                    }
                }
            }
    }

}
