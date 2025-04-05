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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.view.View
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.systemui.base.lockscreen.Keyguard.keyguardBottomAreaInjector

object RemoveCamera : BaseHook() {
    override fun init() {
        // 屏蔽右下角组件显示
        keyguardBottomAreaInjector.methodFinder().filter {
            name in setOf(
                "updateRightAffordanceViewLayoutVisibility",
                "startButtonLayoutAnimate"
            )
        }.toList().createHooks {
            after {
                val right =
                    ObjectUtils.getObjectOrNullAs<LinearLayout>(it.thisObject, "mRightAffordanceViewLayout") ?: return@after
                right.visibility = View.GONE
            }
        }

        // 屏蔽滑动撞墙动画
        loadClassOrNull("com.android.keyguard.KeyguardMoveRightController")!!.methodFinder()
            .filterByName("onTouchMove")
            .filterByParamCount(2)
            .single().createHook {
                returnConstant(false)
            }
        loadClassOrNull("com.android.keyguard.KeyguardMoveRightController")!!.methodFinder()
            .filterByName("reset")
            .single().createHook {
                returnConstant(null)
            }
    }
}
