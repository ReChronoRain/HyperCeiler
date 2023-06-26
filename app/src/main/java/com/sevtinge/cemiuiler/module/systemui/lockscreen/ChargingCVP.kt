package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.devicesdk.isMoreAndroidVersion
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object ChargingCVP : BaseHook() {
    @SuppressLint("SetTextI18n")
    override fun init() {
        if (isMoreAndroidVersion(33)) {
            val clazzDependency = loadClass("com.android.systemui.Dependency")
            val clazzKeyguardIndicationController =
                loadClass("com.android.systemui.statusbar.KeyguardIndicationController")
            loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors
             ?.createHooks {
                after { param ->
                    (param.thisObject as TextView).isSingleLine = false
                    val screenOnOffReceiver = @SuppressLint("ServiceCast")
                    object : BroadcastReceiver() {
                        val keyguardIndicationController = invokeStaticMethodBestMatch(
                            clazzDependency, "get", null, clazzKeyguardIndicationController)!!
                        val handler = Handler((param.thisObject as TextView).context.mainLooper)
                        val runnable = object : Runnable {
                            override fun run() {
                                invokeMethodBestMatch(keyguardIndicationController, "updatePowerIndication")
                                handler.postDelayed(
                                    this, mPrefsMap.getInt("system_ui_statusbar_lock_screen_show_spacing", 6) / 2 * 1000L
                                )
                            }
                        }

                        init {
                            if (((param.thisObject as TextView).context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
                                handler.post(runnable)
                            }
                        }

                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                Intent.ACTION_SCREEN_ON -> {
                                    handler.post(runnable)
                                }

                                Intent.ACTION_SCREEN_OFF -> {
                                    handler.removeCallbacks(runnable)
                                }
                            }
                        }
                    }

                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                    (param.thisObject as TextView).context.registerReceiver(screenOnOffReceiver, filter)
                }
            }
        } else {
            loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.createHooks {
                after {
                    (it.thisObject as TextView).isSingleLine = false
                }
            }
        }
        loadClass("com.android.keyguard.charge.ChargeUtils").methodFinder()
            .filterByName("getChargingHintText")
            .filterByParamTypes(
                Context::class.java,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            .first().createHook {
                after {
                    if (it.result != null) {
                        it.result = it.result as String + getCVP()
                    }
                }
        }
    }

    private fun getCVP(): String {
        // 获取电流信息
        val batteryManager =
            AndroidAppHelper.currentApplication()
                .getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current =
            abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
        var voltage = 0.0
        var temp = 0.0

        kotlin.runCatching {
            // 获取电压信息
            val voltageNow =
                BufferedReader(FileReader("/sys/class/power_supply/battery/voltage_now"))
            voltage =
                BigDecimal(voltageNow.readLine().toDouble() / 1000.0).setScale(1, RoundingMode.HALF_UP).toDouble()
           // 获取电池温度信息
            val temNow =
                BufferedReader(FileReader("/sys/class/power_supply/battery/temp"))
            temp =
                BigDecimal(temNow.readLine().toDouble() / 10.0).setScale(1, RoundingMode.HALF_UP).toDouble()
        }

        // 计算功率信息
        val powerAll = abs((current * voltage) / 1000f / 1000f)
        val power = String.format("%.2f", powerAll)

        // 电流展示逻辑设置
        val mCurrent = when (mPrefsMap.getBoolean("system_ui_show_charging_c_more")) {
            true -> "$current mA"
            else -> "${String.format("%.1f", abs(current / 1000f))} A"
        }
        val mVoltage = "${String.format("%.1f", abs(voltage / 1000f))} V"
        // 电池温度是否展示
        val mTemp = if (mPrefsMap.getBoolean("system_ui_show_battery_temperature")) " · $temp ℃" else ""
        // 输出展示信息
        return "${mTemp}\n$mCurrent · $mVoltage · $power W"
    }
}
