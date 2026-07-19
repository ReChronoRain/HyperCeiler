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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import android.view.View
import android.widget.LinearLayout
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard.keyguardBottomAreaInjector
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

object RemoveCamera : BaseHook() {
    override fun init() {
        // 屏蔽右下角组件显示
        keyguardBottomAreaInjector.findAllMethods { filter { name in setOf(
                "updateRightAffordanceViewLayoutVisibility",
                "startButtonLayoutAnimate"
            ) } }.createHooks {
            after {
                val right =
                    it.thisObject.getObjectFieldOrNullAs<LinearLayout>("mRightAffordanceViewLayout") ?: return@after
                right.visibility = View.GONE
            }
        }

        // 屏蔽滑动撞墙动画
        loadClassOrNull("com.android.keyguard.KeyguardMoveRightController")!!.findMethod { name("onTouchMove"); paramCount(2) }.createHook {
                returnConstant(false)
            }
        loadClassOrNull("com.android.keyguard.KeyguardMoveRightController")!!.findMethod { name("reset") }.createHook {
                returnConstant(null)
            }
    }
}
