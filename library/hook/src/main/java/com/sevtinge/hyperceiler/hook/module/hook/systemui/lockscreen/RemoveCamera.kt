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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen

import android.view.View
import android.widget.LinearLayout
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.lockscreen.Keyguard.keyguardBottomAreaInjector
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

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
                    it.thisObject.getObjectFieldOrNullAs<LinearLayout>("mRightAffordanceViewLayout") ?: return@after
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
