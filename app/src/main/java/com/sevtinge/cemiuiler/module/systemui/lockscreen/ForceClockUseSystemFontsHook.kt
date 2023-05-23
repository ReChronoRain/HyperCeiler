package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.graphics.Typeface
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object ForceClockUseSystemFontsHook : BaseHook() {
    override fun init() {
        Helpers.findAndHookMethod(
            "com.miui.clock.MiuiBaseClock", lpparam.classLoader,
            "updateViewsTextSize",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val mTimeText =
                        XposedHelpers.getObjectField(param.thisObject, "mTimeText") as TextView
                    mTimeText.typeface = Typeface.DEFAULT
                }
            })
        Helpers.findAndHookMethod(
            "com.miui.clock.MiuiLeftTopLargeClock", lpparam.classLoader,
            "onLanguageChanged",
            String::class.java,
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val mTimeText = XposedHelpers.getObjectField(
                        param.thisObject,
                        "mCurrentDateLarge"
                    ) as TextView
                    mTimeText.typeface = Typeface.DEFAULT
                }
            })
    }
}