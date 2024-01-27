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

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField
import de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField

object LockScreenDoubleTapToSleep : BaseHook() {
    override fun init() {
        loadClass(
            if (isAndroidVersion(34))
                "com.android.systemui.shade.NotificationsQuickSettingsContainer"
            else
                "com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer").methodFinder().first {
            name == "onFinishInflate"
        }.createHook {
            before {
                val view = it.thisObject as View
                setAdditionalInstanceField(view, "currentTouchTime", 0L)
                setAdditionalInstanceField(view, "currentTouchX", 0f)
                setAdditionalInstanceField(view, "currentTouchY", 0f)
                view.setOnTouchListener(View.OnTouchListener { v, event ->
                    if (event.action != MotionEvent.ACTION_DOWN) return@OnTouchListener false

                    var currentTouchTime =
                         getAdditionalInstanceField(view, "currentTouchTime") as Long
                    var currentTouchX =
                        getAdditionalInstanceField(view, "currentTouchX") as Float
                    var currentTouchY =
                        getAdditionalInstanceField(view, "currentTouchY") as Float
                    val lastTouchTime = currentTouchTime
                    val lastTouchX = currentTouchX
                    val lastTouchY = currentTouchY

                    currentTouchTime = System.currentTimeMillis()
                    currentTouchX = event.x
                    currentTouchY = event.y

                    if (currentTouchTime - lastTouchTime < 250L
                        && kotlin.math.abs(currentTouchX - lastTouchX) < 100f
                        && kotlin.math.abs(currentTouchY - lastTouchY) < 100f
                    ) {
                        val keyguardMgr =
                            v.context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                        if (keyguardMgr.isKeyguardLocked) {
                            XposedHelpers.callMethod(
                                v.context.getSystemService(Context.POWER_SERVICE),
                                "goToSleep",
                                SystemClock.uptimeMillis()
                            )
                        }
                        currentTouchTime = 0L
                        currentTouchX = 0f
                        currentTouchY = 0f
                    }

                    setAdditionalInstanceField(view, "currentTouchTime", currentTouchTime)
                    setAdditionalInstanceField(view, "currentTouchX", currentTouchX)
                    setAdditionalInstanceField(view, "currentTouchY", currentTouchY)
                    v.performClick()
                    false
                })
            }
        }
    }

}
