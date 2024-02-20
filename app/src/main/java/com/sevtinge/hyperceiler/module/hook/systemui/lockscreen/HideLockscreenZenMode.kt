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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.setObjectField

object HideLockscreenZenMode : BaseHook() {
    override fun init() {
        // hyperOS fix by hyper helper
        if (isMoreHyperOSVersion(1f)) {
            loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader)
                .methodFinder()
                .filterByName("updateVisibility")
                .single().createHook {
                    before {
                        it.thisObject.setObjectField("manuallyDismissed", true)
                    }
                }
        } else {
            loadClass("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader)
                .methodFinder()
                .filterByName("shouldBeVisible")
                .single().createHook {
                    returnConstant(false)
                }
        }
    }
}
