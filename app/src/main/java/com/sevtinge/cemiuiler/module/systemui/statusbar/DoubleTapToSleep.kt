package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers
import kotlin.math.abs

object DoubleTapToSleep : BaseHook() {

    override fun init() {
        findMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView") {
            name == "onFinishInflate"
        }.hookBefore {
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
                if (currentTouchTime - lastTouchTime < 250L && abs(currentTouchX - lastTouchX) < 100f && abs(
                        currentTouchY - lastTouchY
                    ) < 100f
                ) {
                    XposedHelpers.callMethod(
                        v.context.getSystemService(Context.POWER_SERVICE),
                        "goToSleep",
                        SystemClock.uptimeMillis()
                    )
                    currentTouchTime = 0L
                    currentTouchX = 0f
                    currentTouchY = 0f
                }
                XposedHelpers.setAdditionalInstanceField(
                    view,
                    "currentTouchTime",
                    currentTouchTime
                )
                XposedHelpers.setAdditionalInstanceField(view, "currentTouchX", currentTouchX)
                XposedHelpers.setAdditionalInstanceField(view, "currentTouchY", currentTouchY)
                v.performClick()
                false
            })
        }
    }

}