package com.sevtinge.cemiuiler.module.hook.systemui.lockscreen

import android.graphics.Typeface
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectFieldAs


object ForceClockUseSystemFontsHook : BaseHook() {
    override fun init() {

        loadClass("com.miui.clock.MiuiBaseClock").methodFinder().filter {
            name == "updateViewsTextSize"
        }.toList().createHooks {
            after { param ->
                val mTimeText =
                    param.thisObject.getObjectFieldAs<TextView>("mTimeText")
                mTimeText.typeface = Typeface.DEFAULT
            }
        }

        loadClass("com.miui.clock.MiuiLeftTopLargeClock").methodFinder().filter {
            name == "onLanguageChanged" && parameterTypes == String::class.java
        }.toList().createHooks {
            after { param ->
                val mTimeText =
                    param.thisObject.getObjectFieldAs<TextView>("mCurrentDateLarge")
                mTimeText.typeface = Typeface.DEFAULT
            }
        }
    }
}
