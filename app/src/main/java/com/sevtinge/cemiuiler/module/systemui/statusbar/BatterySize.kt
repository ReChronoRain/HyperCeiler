package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.util.TypedValue
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectFieldAs

object BatterySize : BaseHook() {
    override fun init() {
        val size = mPrefsMap.getInt("system_ui_statusbar_battery_size", 0).toFloat()
        if (size == 0f) return
        loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView").methodFinder()
            .filterByName("updateResources")
            .first().createHook {
                after {
                    (it.thisObject.getObjectFieldAs<TextView>("mBatteryPercentView")).setTextSize(
                        TypedValue.COMPLEX_UNIT_DIP, size
                    )
                }
            }
    }
}
