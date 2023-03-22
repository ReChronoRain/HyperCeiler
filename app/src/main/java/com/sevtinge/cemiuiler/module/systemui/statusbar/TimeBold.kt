package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.graphics.Typeface
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object TimeBold : BaseHook() {
    private fun initClockStyle(mClock: TextView) {
        if (mPrefsMap.getBoolean("system_statusbar_clock_bold")) {
            mClock.typeface = Typeface.DEFAULT_BOLD
        }
    }

    override fun init() {
        Helpers.findAndHookMethod(
            "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            lpparam.classLoader,
            "onAttachedToWindow",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val clock = XposedHelpers.getObjectField(param.thisObject, "mMiuiClock") as TextView
                    initClockStyle(clock)
                }
            }
        )
    }
}