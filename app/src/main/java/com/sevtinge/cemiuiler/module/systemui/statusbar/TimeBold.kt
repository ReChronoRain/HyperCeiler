package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.graphics.Typeface
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.sevtinge.cemiuiler.module.base.BaseHook


object TimeBold : BaseHook() {
    override fun init() {
       /*Helpers.findAndHookMethod(
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
        )*/
        findMethod("com.android.systemui.statusbar.views.MiuiClock") {
            paramCount == 3
        }.hookAfter {
            val textV = it.thisObject as TextView
            textV.typeface = Typeface.DEFAULT_BOLD
        }
    }
}