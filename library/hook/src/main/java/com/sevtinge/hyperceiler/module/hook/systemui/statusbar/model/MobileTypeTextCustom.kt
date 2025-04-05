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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.module.base.BaseHook

object MobileTypeTextCustom : BaseHook() {
    override fun init() {
        if (isMoreAndroidVersion(35)) {
            loadClass("com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl")
        } else {
            loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
        }.methodFinder()
            .filterByName("getMobileTypeName")
            .filterByParamTypes {
                it[0] == Int::class.java
            }.single().createHook {
                after {
                    it.result =
                        mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", "ERR")
                }
            }
    }
}
