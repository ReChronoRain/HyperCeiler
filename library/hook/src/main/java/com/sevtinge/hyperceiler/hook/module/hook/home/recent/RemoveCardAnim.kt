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
package com.sevtinge.hyperceiler.hook.module.hook.home.recent

import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import com.sevtinge.hyperceiler.hook.utils.setObjectField

object RemoveCardAnim : BaseHook() {
    override fun init() {

        "com.miui.home.recents.views.SwipeHelperForRecents".hookAfterMethod("onTouchEvent", MotionEvent::class.java) {
            if (it.thisObject.getObjectField("mCurrView") != null) {
                val taskView2 = it.thisObject.getObjectField("mCurrView") as View
                taskView2.alpha = 1f
                taskView2.scaleX = 1f
                taskView2.scaleY = 1f
            }
        }
        "com.miui.home.recents.TaskStackViewLayoutStyleHorizontal".replaceMethod(
            "createScaleDismissAnimation", View::class.java, Float::class.java
        ) {
            val view = it.args[0] as View
            val getScreenHeight =
                findClass("com.miui.home.launcher.DeviceConfig").callStaticMethod("getScreenHeight") as Int
            val ofFloat =
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, -getScreenHeight * 1.1484375f)
            ofFloat.duration = 200
            return@replaceMethod ofFloat
        }

        "com.miui.home.recents.views.VerticalSwipe".hookAfterMethod("calculate", Float::class.java) {
            val f = it.args[0] as Float
            val asScreenHeightWhenDismiss =
                "com.miui.home.recents.views.VerticalSwipe".findClass()
                    .callStaticMethod("getAsScreenHeightWhenDismiss") as Int
            val f2 = f / asScreenHeightWhenDismiss
            val mTaskViewHeight = it.thisObject.getObjectField("mTaskViewHeight") as Float
            val mCurScale = it.thisObject.getObjectField("mCurScale") as Float
            val f3: Float = mTaskViewHeight * mCurScale
            val i = if (f2 > 0.0f) 1 else if (f2 == 0.0f) 0 else -1
            val afterFrictionValue: Float =
                it.thisObject.callMethod("afterFrictionValue", f, asScreenHeightWhenDismiss) as Float
            if (i < 0) it.thisObject.setObjectField(
                "mCurTransY",
                (mTaskViewHeight / 2.0f + afterFrictionValue * 2) - (f3 / 2.0f)
            )
        }

    }
}
