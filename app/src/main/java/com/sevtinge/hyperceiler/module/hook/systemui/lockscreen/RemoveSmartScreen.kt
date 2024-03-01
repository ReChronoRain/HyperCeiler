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

import android.view.View
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion

object RemoveSmartScreen : BaseHook() {
    override fun init() {
        if (isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34)) {
            loadClassOrNull("com.android.keyguard.injector.KeyguardBottomAreaInjector")!!.methodFinder()
                .filterByName("updateIcons")
                .single().createHook {
                    after {
                        val left =
                            ObjectUtils.getObjectOrNullAs<LinearLayout>(it.thisObject, "mLeftAffordanceViewLayout") ?: return@after
                        left.visibility = View.GONE
                    }
                }
        } else {
            loadClassOrNull("com.android.keyguard.negative.MiuiKeyguardMoveLeftViewContainer")!!.methodFinder()
                .filterByName("inflateLeftView")
                .single().createHook {
                    returnConstant(null)
                }
        }
    }

}
