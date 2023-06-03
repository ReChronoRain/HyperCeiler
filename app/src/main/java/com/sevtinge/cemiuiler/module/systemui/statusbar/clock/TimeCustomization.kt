package com.sevtinge.cemiuiler.module.systemui.statusbar.clock

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectField
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

object TimeCustomization : BaseHook(){
    //预设模式
    private val getMode = mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0)
    private val getClockSize = mPrefsMap.getInt("system_ui_statusbar_clock_size", 0)
    private val getClockDoubleSize = mPrefsMap.getInt("system_ui_statusbar_clock_double_size", 0)
    private val isYear = mPrefsMap.getBoolean("system_ui_statusbar_clock_year")
    private val isMonth = mPrefsMap.getBoolean("system_ui_statusbar_clock_month")
    private val isDay = mPrefsMap.getBoolean("system_ui_statusbar_clock_date")
    private val isWeek = mPrefsMap.getBoolean("system_ui_statusbar_clock_week")
    private val isHideSpace = mPrefsMap.getBoolean("system_ui_statusbar_clock_hide_space")
    private val isDoubleLine = mPrefsMap.getBoolean("system_ui_statusbar_clock_double")
    private val isSecond = mPrefsMap.getBoolean("system_ui_statusbar_clock_second")
    private val isDoubleHour = mPrefsMap.getBoolean("system_ui_statusbar_clock_hour_cn")
    private val isPeriod = mPrefsMap.getBoolean("system_ui_statusbar_clock_period")
    private val isCenterAlign = mPrefsMap.getBoolean("system_ui_statusbar_clock_double_center")

    //极客模式
    private val getGeekClockSize = mPrefsMap.getInt("system_ui_statusbar_clock_size_geek", 0)
    private val getGeekFormat = mPrefsMap.getString("system_ui_statusbar_clock_editor", "HH:mm:ss")
    private val isGeekCenterAlign = mPrefsMap.getBoolean("system_ui_statusbar_clock_center_geek")

    private lateinit var nowTime: Date
    private var str = ""

    @SuppressLint("SetTextI18n")
    override fun init() {
        val mClockClass = when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R ->  loadClass("com.android.systemui.statusbar.policy.MiuiClock")
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->  loadClass("com.android.systemui.statusbar.views.MiuiClock")
            else -> null
        }

        when (getMode) {
            //预设模式
            1 -> {
                var c: Context? = null
                mClockClass?.constructorFinder()?.first {
                    paramCount == 3
                }?.createHook {
                    after {
                        try {
                            c = it.args[0] as Context
                            val textV = it.thisObject as TextView
                            if (textV.resources.getResourceEntryName(textV.id) != "clock") return@after
                            textV.isSingleLine = false
                            if (isDoubleLine) {
                                str = "\n"
                                var clockDoubleLineSize = 7F
                                if (getClockDoubleSize != 0) {
                                    clockDoubleLineSize = getClockDoubleSize.toFloat()
                                }
                                textV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockDoubleLineSize)
                                textV.setLineSpacing(0F, 0.8F)
                            } else {
                                if (getClockSize != 0) {
                                    val clockSize = getClockSize.toFloat()
                                    textV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSize)
                                }
                            }
                            val d: Method = textV.javaClass.getDeclaredMethod("updateTime")
                            val r = Runnable {
                                d.isAccessible = true
                                d.invoke(textV)
                            }

                            class T : TimerTask() {
                                override fun run() {
                                    Handler(textV.context.mainLooper).post(r)
                                }
                            }
                            Timer().scheduleAtFixedRate(
                                T(), 1000 - System.currentTimeMillis() % 1000, 1000
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                mClockClass?.methodFinder()?.first {
                    name == "updateTime"
                }?.createHook {
                    after {
                        try {
                            val textV = it.thisObject as TextView
                            if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                                val t = Settings.System.getString(
                                    c!!.contentResolver, Settings.System.TIME_12_24
                                )
                                val is24 = t == "24"
                                nowTime = Calendar.getInstance().time
                                textV.text = getDate(c!!) + str + getTime(c!!, is24)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                if (isCenterAlign) {
                   mClockClass?.constructorFinder()?.first {
                        paramCount == 3
                   }?.createHook {
                       after {
                           try {
                               val textV = it.thisObject as TextView
                               if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                                   c = it.args[0] as Context
                                   textV.gravity = Gravity.CENTER
                               }
                           } catch (_: Exception) {
                           }
                       }
                   }
                }
            }
            //极客模式
            2 -> {
                var c: Context? = null

                mClockClass?.constructorFinder()?.first {
                    paramCount == 3
                }?.createHook {
                    after {
                        try {
                            c = it.args[0] as Context
                            val textV = it.thisObject as TextView
                            if (textV.resources.getResourceEntryName(textV.id) != "clock") return@after
                            textV.isSingleLine = false
                            textV.setLineSpacing(0F, 0.8F)
                            if (getGeekClockSize != 0) {
                                val clockSize = getGeekClockSize.toFloat()
                                textV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSize)
                            }

                            val d: Method = textV.javaClass.getDeclaredMethod("updateTime")
                            val r = Runnable {
                                d.isAccessible = true
                                d.invoke(textV)
                            }

                            class T : TimerTask() {
                                override fun run() {
                                    Handler(textV.context.mainLooper).post(r)
                                }
                            }
                            Timer().scheduleAtFixedRate(
                                T(), 1000 - System.currentTimeMillis() % 1000, 1000
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                mClockClass?.methodFinder()?.first {
                    name == "updateTime"
                }?.createHook {
                    before {
                        try {
                            val textV = it.thisObject as TextView
                            if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                                val mMiuiStatusBarClockController = textV.getObjectField("mMiuiStatusBarClockController")
                                val mCalendar = mMiuiStatusBarClockController?.callMethod("getCalendar")
                                mCalendar?.callMethod(
                                    "setTimeInMillis", System.currentTimeMillis()
                                )
                                val textSb = StringBuilder()
                                val formatSb = StringBuilder(getGeekFormat.toString())
                                mCalendar?.callMethod("format", c, textSb, formatSb)
                                textV.text = textSb.toString()
                                it.result = null
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                if (isGeekCenterAlign) {
                    mClockClass?.constructorFinder()?.first {
                        paramCount == 3
                    }?.createHook {
                        after {
                            try {
                                val textV = it.thisObject as TextView
                                if (textV.resources.getResourceEntryName(textV.id) == "clock") {
                                    c = it.args[0] as Context
                                    textV.gravity = Gravity.CENTER
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDate(context: Context): String {
        var datePattern = ""
        val isZh = isZh(context)

        if (isYear) {
            if (isZh) {
                datePattern += "YY年"
            } else {
                datePattern += "YY"
                if (isMonth || isDay) datePattern += "/"
            }
        }
        if (isMonth) {
            if (isZh) {
                datePattern += "M月"
            } else {
                datePattern += "M"
                if (isDay) datePattern += "/"
            }
        }
        if (isDay) {
            datePattern += if (isZh) {
                "d日"
            } else {
                "d"
            }
        }
        if (isWeek) {
            if (!isHideSpace) datePattern = "$datePattern "
            datePattern += "E"
            if (!isDoubleLine) {
                if (!isHideSpace) datePattern = "$datePattern "
            }
        }
        datePattern = SimpleDateFormat(datePattern).format(nowTime)
        return datePattern
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTime(context: Context, t: Boolean): String {
        var timePattern = ""
        val isZh = isZh(context)
        timePattern += if (t) "HH:mm" else "h:mm"
        if (isSecond) timePattern += ":ss"
        timePattern = SimpleDateFormat(timePattern).format(nowTime)
        if (isZh) timePattern = getPeriod(isZh) + timePattern else timePattern += getPeriod(isZh)
        timePattern = getDoubleHour() + timePattern
        return timePattern
    }

    @SuppressLint("SimpleDateFormat")
    private fun getPeriod(isZh: Boolean): String {
        var period = ""
        if (isPeriod) {
            if (isZh) {
                when (SimpleDateFormat("HH").format(nowTime)) {
                    "00", "01", "02", "03", "04", "05" -> {
                        period = "凌晨"
                    }
                    "06", "07", "08", "09", "10", "11" -> {
                        period = "上午"
                    }
                    "12" -> {
                        period = "中午"
                    }
                    "13", "14", "15", "16", "17" -> {
                        period = "下午"
                    }
                    "18" -> {
                        period = "傍晚"
                    }
                    "19", "20", "21", "22", "23" -> {
                        period = "晚上"
                    }
                }
            } else {
                period = SimpleDateFormat("a").format(nowTime)
                if (!isHideSpace) {
                    period = " $period"
                }
            }

        }
        return period
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDoubleHour(): String {
        var doubleHour = ""
        if (isDoubleHour) {
            when (SimpleDateFormat("HH").format(nowTime)) {
                "23", "00" -> {
                    doubleHour = "子时"
                }
                "01", "02" -> {
                    doubleHour = "丑时"
                }
                "03", "04" -> {
                    doubleHour = "寅时"
                }
                "05", "06" -> {
                    doubleHour = "卯时"
                }
                "07", "08" -> {
                    doubleHour = "辰时"
                }
                "09", "10" -> {
                    doubleHour = "巳时"
                }
                "11", "12" -> {
                    doubleHour = "午时"
                }
                "13", "14" -> {
                    doubleHour = "未时"
                }
                "15", "16" -> {
                    doubleHour = "申时"
                }
                "17", "18" -> {
                    doubleHour = "酉时"
                }
                "19", "20" -> {
                    doubleHour = "戌时"
                }
                "21", "22" -> {
                    doubleHour = "亥时"
                }
            }
            if (!isHideSpace) {
                doubleHour += " "
            }
        }
        return doubleHour
    }

    private fun isZh(context: Context): Boolean {
        return context.resources.configuration.locales.get(0).language.endsWith("zh")
    }
}
