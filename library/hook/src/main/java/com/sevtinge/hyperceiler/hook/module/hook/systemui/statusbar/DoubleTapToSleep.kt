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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers
import kotlin.math.abs

object DoubleTapToSleep : BaseHook() {

    override fun init() {
        loadClass("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView").methodFinder()
            .filterByName("onFinishInflate")
            .single().createHook {
                before {
                    val view = it.thisObject as ViewGroup
                    XposedHelpers.setAdditionalInstanceField(view, "currentTouchTime", 0L)
                    XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", 0f)
                    XposedHelpers.setAdditionalInstanceField(view, "currentTouchY", 0f)
                    view.setOnTouchListener(View.OnTouchListener { v, event ->
                        if (event.action != MotionEvent.ACTION_DOWN) return@OnTouchListener false
                        var currentTouchTime =
                            XposedHelpers.getAdditionalInstanceField(view, "currentTouchTime") as Long
                        var currentTouchX =
                            XposedHelpers.getAdditionalInstanceField(view, "currentTouchX") as Float
                        var currentTouchY =
                            XposedHelpers.getAdditionalInstanceField(view, "currentTouchY") as Float
                        val lastTouchTime = currentTouchTime
                        val lastTouchX = currentTouchX
                        val lastTouchY = currentTouchY
                        currentTouchTime = System.currentTimeMillis()
                        currentTouchX = event.x
                        currentTouchY = event.y
                        if (currentTouchTime - lastTouchTime < 250L &&
                            abs(currentTouchX - lastTouchX) < 100f &&
                            abs(currentTouchY - lastTouchY) < 100f) {
                            XposedHelpers.callMethod(
                                v.context.getSystemService(Context.POWER_SERVICE),
                                "goToSleep",
                                SystemClock.uptimeMillis()
                            )
                            currentTouchTime = 0L
                            currentTouchX = 0f
                            currentTouchY = 0f
                        }
                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime)
                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX)
                        XposedHelpers.setAdditionalInstanceField(view, "currentTouchY", currentTouchY)
                        v.performClick()
                        false
                    })
                }
            }
    }

}
