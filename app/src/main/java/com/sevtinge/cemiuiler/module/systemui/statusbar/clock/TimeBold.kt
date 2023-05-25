package com.sevtinge.cemiuiler.module.systemui.statusbar.clock

import android.graphics.Typeface
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object TimeBold : BaseHook() {
    override fun init() {
        Helpers.findAndHookMethod(
            "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            lpparam.classLoader,
            "onAttachedToWindow",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val clock =
                        XposedHelpers.getObjectField(param.thisObject, "mMiuiClock") as TextView
                    val clockName =
                        XposedHelpers.getAdditionalInstanceField(clock, "clockName") as String
                    val statusBarClock = clockName == "clock"
                    if (!statusBarClock) {
                        clock.typeface = Typeface.DEFAULT_BOLD
                    }
                }
            }
        )
    }
}