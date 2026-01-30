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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import kotlin.math.abs

object DoubleTapToSleep : BaseHook() {

    private const val DOUBLE_TAP_TIMEOUT = 250L
    private const val TOUCH_SLOP = 100f

    override fun init() {
        loadClass("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView")
            .afterHookMethod("onFinishInflate") { param ->
                val view = param.thisObject as ViewGroup
                setupDoubleTapListener(view)
            }
    }

    private fun setupDoubleTapListener(view: ViewGroup) {
        view.setAdditionalInstanceField("lastTouchTime", 0L)
        view.setAdditionalInstanceField("lastTouchX", 0f)
        view.setAdditionalInstanceField("lastTouchY", 0f)

        view.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener false

            val lastTime = v.getAdditionalInstanceField("lastTouchTime") as? Long ?: 0L
            val lastX = v.getAdditionalInstanceField("lastTouchX") as? Float ?: 0f
            val lastY = v.getAdditionalInstanceField("lastTouchY") as? Float ?: 0f

            val currentTime = System.currentTimeMillis()
            val currentX = event.x
            val currentY = event.y

            val isDoubleTap = currentTime - lastTime < DOUBLE_TAP_TIMEOUT
                && abs(currentX - lastX) < TOUCH_SLOP
                && abs(currentY - lastY) < TOUCH_SLOP

            if (isDoubleTap) {
                goToSleep(v.context)
                v.setAdditionalInstanceField("lastTouchTime", 0L)
                v.setAdditionalInstanceField("lastTouchX", 0f)
                v.setAdditionalInstanceField("lastTouchY", 0f)
            } else {
                v.setAdditionalInstanceField("lastTouchTime", currentTime)
                v.setAdditionalInstanceField("lastTouchX", currentX)
                v.setAdditionalInstanceField("lastTouchY", currentY)
            }

            v.performClick()
            false
        }
    }

    private fun goToSleep(context: Context) {
        runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE)
            powerManager?.callMethod("goToSleep", SystemClock.uptimeMillis())
        }
    }
}

