package com.sevtinge.cemiuiler.module.systemui.statusbar.clock

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.sevtinge.cemiuiler.module.base.BaseHook


object TimeStyle : BaseHook() {
    @SuppressLint("RtlHardcoded")
    override fun init() {
        val mClockClass = when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R ->  loadClass("com.android.systemui.statusbar.policy.MiuiClock")
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->  loadClass("com.android.systemui.statusbar.views.MiuiClock")
            else -> null
        }
        val clockBold = mPrefsMap.getBoolean("system_ui_statusbar_clock_bold")
        val getMode = mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0)
        val isAlign = mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_mode", 0)
        val isGeekAlign = mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_mode_geek", 0)

        // 时钟加粗
        if (clockBold) {
            mClockClass?.constructorFinder()?.first {
                paramCount == 3
            }?.createHook {
                after {
                    val mClock = it.thisObject as TextView
                    mClock.typeface = Typeface.DEFAULT_BOLD
                }
            }
        }

        // 时钟对齐方式
        mClockClass?.constructorFinder()?.first {
            paramCount == 3
        }?.createHook {
            after {
                try {
                    val textV = it.thisObject as TextView
                    if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                       when (getMode) {
                           1 -> {
                               textV.gravity = when (isAlign) {
                                   1 -> Gravity.CENTER
                                   2 -> Gravity.RIGHT
                                   else -> Gravity.LEFT
                               }
                           }
                           2 -> {
                               textV.gravity = when (isGeekAlign) {
                                   1 -> Gravity.CENTER
                                   2 -> Gravity.RIGHT
                                   else -> Gravity.LEFT
                               }
                           }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 时钟边距调整（暂时是饼）
    }
}