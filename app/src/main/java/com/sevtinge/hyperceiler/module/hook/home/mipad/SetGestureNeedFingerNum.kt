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
package com.sevtinge.hyperceiler.module.hook.home.mipad

import android.view.MotionEvent
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object SetGestureNeedFingerNum : BaseHook() {
    override fun init() {
        val clazzGestureOperationHelper =
            loadClass("com.miui.home.recents.GestureOperationHelper")
        clazzGestureOperationHelper.methodFinder()
            .filterByName("isThreePointerSwipeLeftOrRightInScreen")
            .filterByParamTypes(MotionEvent::class.java, Int::class.java)
            .first().createHook {
                before { param ->
                    val motionEvent = param.args[0] as MotionEvent
                    val swipeFlag = param.args[1] as Int
                    val flagSwipeLeft =
                        getStaticObjectOrNullAs<Int>(clazzGestureOperationHelper, "SWIPE_DIRECTION_LEFT")
                    val flagSwipeRight =
                        getStaticObjectOrNullAs<Int>(clazzGestureOperationHelper, "SWIPE_DIRECTION_RIGHT")
                    val flagsSwipeLeftAndRight = setOf(flagSwipeLeft, flagSwipeRight)
                    val z =
                        if (motionEvent.device == null) true
                        else motionEvent.device.sources and 4098 == 4098
                    param.result =
                        z && (swipeFlag in flagsSwipeLeftAndRight) && motionEvent.pointerCount == 4
                }
            }
    }

}
