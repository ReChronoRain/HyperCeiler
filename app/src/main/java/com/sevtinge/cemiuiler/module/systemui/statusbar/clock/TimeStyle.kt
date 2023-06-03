package com.sevtinge.cemiuiler.module.systemui.statusbar.clock

import android.graphics.Typeface
import android.os.Build
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.sevtinge.cemiuiler.module.base.BaseHook


object TimeStyle : BaseHook() {
    override fun init() {
        val mClockClass = when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R ->  loadClass("com.android.systemui.statusbar.policy.MiuiClock")
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->  loadClass("com.android.systemui.statusbar.views.MiuiClock")
            else -> null
        }
        mClockClass?.constructorFinder()?.first {
            paramCount == 3
        }?.createHook {
            after {
                val mClock = it.thisObject as TextView
                mClock.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }
}