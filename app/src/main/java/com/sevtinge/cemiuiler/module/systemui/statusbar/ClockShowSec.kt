package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import android.text.format.DateFormat
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.SdkHelper
import de.robv.android.xposed.XposedHelpers
import java.util.*


object ClockShowSec : BaseHook() {

    private fun getShowSeconds(): Boolean {
        val sbShowSeconds: Boolean = mPrefsMap.getBoolean("system_ui_statusbar_clock_show_second")
        val customFormat: String = mPrefsMap.getString("system_ui_statusbar_clock_diy", "")
        val enableCustomFormat: Boolean =
            mPrefsMap.getBoolean("system_ui_statusbar_clock_diy_status")
        return enableCustomFormat && customFormat.contains("ss") || !enableCustomFormat && sbShowSeconds
    }

    private fun initSecondTimer(clockController: Any) {
        val finalSbShowSeconds = getShowSeconds()
        val mContext = XposedHelpers.getObjectField(clockController, "mContext") as Context
        val scheduleTimer: Timer =
            XposedHelpers.getAdditionalInstanceField(clockController, "scheduleTimer") as Timer
        scheduleTimer.cancel()
        if (finalSbShowSeconds) {
            val mClockHandler = android.os.Handler(mContext.mainLooper)
            val delay = 1000 - System.currentTimeMillis() % 1000
            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    mClockHandler.post {
                        val mCalendar = XposedHelpers.getObjectField(clockController, "mCalendar")
                        XposedHelpers.callMethod(
                            mCalendar,
                            "setTimeInMillis",
                            System.currentTimeMillis()
                        )
                        XposedHelpers.setObjectField(
                            clockController,
                            "mIs24",
                            DateFormat.is24HourFormat(mContext)
                        )
                        val mClockListeners = XposedHelpers.getObjectField(
                            clockController,
                            "mClockListeners"
                        ) as ArrayList<*>
                        val it: Iterator<Any> = mClockListeners.iterator()
                        while (it.hasNext()) {
                            val clock = it.next()
                            val showSeconds =
                                XposedHelpers.getAdditionalInstanceField(clock, "showSeconds")
                            if (showSeconds != null) {
                                XposedHelpers.callMethod(clock, "onTimeChange")
                            }
                        }
                    }
                }
            }, delay, 1000)
            XposedHelpers.setAdditionalInstanceField(clockController, "scheduleTimer", timer)
        }
    }

    override fun init() {
        val scheduleHook: Helpers.MethodHook = object : Helpers.MethodHook() {
            @Throws(Throwable::class)
            override fun after(param: MethodHookParam) {
                initSecondTimer(param.thisObject)
                val mContext: Context =
                    XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                val timeSetIntent = IntentFilter()
                timeSetIntent.addAction("android.intent.action.TIME_SET")
                val mUpdateTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        initSecondTimer(param.thisObject)
                    }
                }
                mContext.registerReceiver(mUpdateTimeReceiver, timeSetIntent)
            }
        }
//      状态栏显秒
        if (!SdkHelper.isAndroidR()) {
            val miuiClockClass: Class<*> = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.policy.MiuiStatusBarClockController",
                lpparam.classLoader
            )
            Helpers.hookAllConstructors(miuiClockClass, scheduleHook)
            Helpers.findAndHookMethod(miuiClockClass, "fireTimeChange", object : MethodHook() {
                    @Throws(Throwable::class)
                    override fun before(param: MethodHookParam) {
                        val clockController = param.thisObject
                        val mClockListeners = XposedHelpers.getObjectField(clockController, "mClockListeners") as ArrayList<*>
                        val it: Iterator<Any> = mClockListeners.iterator()
                        while (it.hasNext()) {
                            val clock = it.next()
                            val showSeconds = XposedHelpers.getAdditionalInstanceField(clock, "showSeconds")
                            if (showSeconds == null) {
                                XposedHelpers.callMethod(clock, "onTimeChange")
                            }
                        }
                        param.result = null
                    }
                }
            )
        } else {
            val miuiClockClassR: Class<*> = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.policy.MiuiClock",
                lpparam.classLoader
            )
            Helpers.findAndHookMethod(miuiClockClassR, "updateTime", object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    XposedHelpers.setObjectField(param.thisObject, "mShowSeconds", true)
                    val clock = param.thisObject as TextView
                    val df: NumberFormat = DecimalFormat("00")
                    clock.append(":" + df.format(Calendar.getInstance()[Calendar.SECOND]))
                }
            })
        }

//        if (statusbarClockTweak) {
//            Helpers.findAndHookMethod(
//                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
//                lpparam.classLoader,
//                "onAttachedToWindow",
//                object : MethodHook() {
//                    @Throws(Throwable::class)
//                    override fun after(param: MethodHookParam) {
//                        val clock =
//                            XposedHelpers.getObjectField(param.thisObject, "mMiuiClock") as TextView
//                        initClockStyle(clock)
//                    }
//                })
//        }
//        val ccDateFormat: String = mPrefs.getString("system_cc_dateformat", "")
//        val ccDateCustom = ccDateFormat.length > 0
//        Helpers.hookAllConstructors(
//            "com.android.systemui.statusbar.views.MiuiClock",
//            lpparam.classLoader,
//            object : MethodHook() {
//                override fun after(param: MethodHookParam) {
//                    val clock = param.thisObject as TextView
//                    if (param.args.size != 3) return
//                    val clockId =
//                        clock.resources.getIdentifier("clock", "id", "com.android.systemui")
//                    val bigClockId =
//                        clock.resources.getIdentifier("big_time", "id", "com.android.systemui")
//                    val dateClockId =
//                        clock.resources.getIdentifier("date_time", "id", "com.android.systemui")
//                    val thisClockId = clock.id
//                    if (clockId == thisClockId && statusbarClockTweak) {
//                        XposedHelpers.setAdditionalInstanceField(clock, "clockName", "clock")
//                        if (getShowSeconds()) {
//                            XposedHelpers.setAdditionalInstanceField(clock, "showSeconds", true)
//                        }
//                    } else if (bigClockId == thisClockId && ccShowSeconds) {
//                        XposedHelpers.setAdditionalInstanceField(clock, "clockName", "ccClock")
//                        XposedHelpers.setAdditionalInstanceField(clock, "showSeconds", true)
//                    } else if (dateClockId == thisClockId && ccDateCustom) {
//                        XposedHelpers.setAdditionalInstanceField(clock, "clockName", "ccDate")
//                    }
//                }
//            })
//        Helpers.findAndHookMethod(
//            "com.android.systemui.statusbar.views.MiuiClock",
//            lpparam.classLoader,
//            "updateTime",
//            object : MethodHook(
//                PRIORITY_HIGHEST
//            ) {
//                @Throws(Throwable::class)
//                override fun before(param: MethodHookParam) {
//                    val clock = param.thisObject as TextView
//                    val clockName =
//                        XposedHelpers.getAdditionalInstanceField(clock, "clockName") as String
//                    val mContext: Context = clock.context
//                    val mMiuiStatusBarClockController =
//                        XposedHelpers.getObjectField(clock, "mMiuiStatusBarClockController")
//                    val mCalendar =
//                        XposedHelpers.callMethod(mMiuiStatusBarClockController, "getCalendar")
//                    var timeFmt: String? = null
//                    if ("ccClock" == clockName && ccShowSeconds) {
//                        val fmt: String
//                        val mAmPmStyle = XposedHelpers.getObjectField(clock, "mAmPmStyle") as Int
//                        val is24 = XposedHelpers.callMethod(
//                            mMiuiStatusBarClockController,
//                            "getIs24"
//                        ) as Boolean
//                        fmt = if (is24) {
//                            "fmt_time_24hour_minute"
//                        } else {
//                            if (mAmPmStyle == 0) {
//                                "fmt_time_12hour_minute_pm"
//                            } else {
//                                "fmt_time_12hour_minute"
//                            }
//                        }
//                        val fmtResId: Int = mContext.getResources()
//                            .getIdentifier(fmt, "string", "com.android.systemui")
//                        timeFmt = mContext.getString(fmtResId)
//                        timeFmt = timeFmt.replaceFirst(":mm".toRegex(), ":mm:ss")
//                    } else if ("ccDate" == clockName && ccDateCustom) {
//                        timeFmt = ccDateFormat
//                    } else if ("clock" == clockName && statusbarClockTweak) {
//                        val customFormat: String =
//                            mPrefs.getString("system_statusbar_clock_customformat", "")
//                        var enableCustomFormat: Boolean =
//                            mPrefs.getBoolean("system_statusbar_clock_customformat_enable")
//                        enableCustomFormat = enableCustomFormat && customFormat.length > 0
//                        if (enableCustomFormat) {
//                            timeFmt = customFormat
//                        } else {
//                            val showSeconds: Boolean =
//                                mPrefs.getBoolean("system_statusbar_clock_show_seconds")
//                            val is24: Boolean =
//                                mPrefs.getBoolean("system_statusbar_clock_24hour_format")
//                            val showAmpm: Boolean =
//                                mPrefs.getBoolean("system_statusbar_clock_show_ampm")
//                            val hourIn2d: Boolean =
//                                mPrefs.getBoolean("system_statusbar_clock_leadingzero")
//                            val fmt: String
//                            fmt = if (showAmpm) {
//                                "fmt_time_12hour_minute_pm"
//                            } else {
//                                "fmt_time_12hour_minute"
//                            }
//                            val fmtResId: Int = mContext.getResources()
//                                .getIdentifier(fmt, "string", "com.android.systemui")
//                            timeFmt = mContext.getString(fmtResId)
//                            if (showSeconds) {
//                                timeFmt = timeFmt.replaceFirst(":mm".toRegex(), ":mm:ss")
//                            }
//                            var hourStr = "h"
//                            if (is24) {
//                                hourStr = "H"
//                            }
//                            if (hourIn2d) {
//                                hourStr = hourStr + hourStr
//                            }
//                            timeFmt = timeFmt.replaceFirst("h+:".toRegex(), "$hourStr:")
//                        }
//                    }
//                    if (timeFmt != null) {
//                        val formatSb = StringBuilder(timeFmt)
//                        val textSb = StringBuilder()
//                        XposedHelpers.callMethod(mCalendar, "format", mContext, textSb, formatSb)
//                        clock.text = textSb.toString()
//                        param.result = null
//                    }
//                }
//            }
//        )
    }
}