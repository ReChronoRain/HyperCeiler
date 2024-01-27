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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import de.robv.android.xposed.XC_MethodHook
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

object ClockDisplaySeconds : BaseHook() {
    private var nowTime: Date = Calendar.getInstance().time

    override fun init() {
        loadClass("com.miui.clock.MiuiBaseClock").constructorFinder().first {
            paramCount == 2
        }.createHook {
            after {
                try {
                    val viewGroup = it.thisObject as LinearLayout
                    val d: Method = viewGroup.javaClass.getDeclaredMethod("updateTime")
                    val r = Runnable {
                        d.isAccessible = true
                        d.invoke(viewGroup)
                    }

                    class T : TimerTask() {
                        override fun run() {
                            Handler(viewGroup.context.mainLooper).post(r)
                        }
                    }
                    Timer().scheduleAtFixedRate(T(), 1000 - System.currentTimeMillis() % 1000, 1000)
                } catch (_: Exception) {
                }
            }
        }

        loadClass("com.miui.clock.MiuiLeftTopClock").methodFinder().first {
            name == "updateTime"
        }.createHook {
            after { updateTime(it, false) }
        }

        loadClass("com.miui.clock.MiuiCenterHorizontalClock").methodFinder().first {
            name == "updateTime"
        }.createHook {
            after { updateTime(it, false) }
        }

        loadClass("com.miui.clock.MiuiLeftTopLargeClock").methodFinder().first {
            name == "updateTime"
        }.createHook {
            after { updateTime(it, false) }
        }

        loadClass("com.miui.clock.MiuiVerticalClock").methodFinder().first {
            name == "updateTime"
        }.createHook {
            after { updateTime(it, true) }
        }
    }

    private fun updateTime(it: XC_MethodHook.MethodHookParam, isVertical: Boolean) {
        val textV = it.thisObject.getObjectFieldAs<TextView>("mTimeText")
        val c: Context = textV.context

        Log.d("lock_screen_clock_display_seconds", "updateTime: ${it.thisObject.javaClass.simpleName}")
        val is24 = Settings.System.getString(c.contentResolver, Settings.System.TIME_12_24) == "24"

        nowTime = Calendar.getInstance().time

        textV.text = getTime(is24, isVertical)

    }


    @SuppressLint("SimpleDateFormat")
    private fun getTime(is24: Boolean, isVertical: Boolean): String {
        var timePattern = ""
        timePattern += if (isVertical) { // 垂直
            if (is24) "HH\nmm\nss" else "hh\nmm\nss"
        } else { // 水平
            if (is24) "HH:mm:ss" else "h:mm:ss"
        }
        timePattern = SimpleDateFormat(timePattern).format(nowTime)
        return timePattern
    }
}
