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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

object ControlCenterStyle : BaseHook() {
    override fun init() {
        if (isMoreAndroidVersion(36)) {
            loadClass("com.miui.systemui.controlcenter.data.repository.ControlCenterSettingsRepositoryImpl")
                .constructors.createHooks {
                    after {
                        it.thisObject.setObjectField("forceUseControlCenter", false)
                    }
                }
        } else {
            loadClass("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl")
                .declaredConstructors.createHooks {
                    after {
                        it.thisObject.setObjectField("forceUseControlCenterPanel", false)
                    }
                }
        }
    }
}
