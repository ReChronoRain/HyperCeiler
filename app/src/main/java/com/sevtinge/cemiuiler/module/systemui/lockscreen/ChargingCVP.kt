package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.os.BatteryManager
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object ChargingCVP : BaseHook() {

    @SuppressLint("SetTextI18n")
    override fun init() {

        loadClass("com.android.keyguard.charge.ChargeUtils")
            .methodFinder()
            .filterByName("getChargingHintText")
            .filterByParamCount(3)
            .first().createHook {
            after {
                if (it.result != null) {
                    it.result = it.result as String + "\n" + getCVP()
                }
            }
        }

        loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.createHooks  {
            after {
                (it.thisObject as TextView).isSingleLine = false
            }
        }
    }

    private fun getCVP(): String {
        val batteryManager =
            AndroidAppHelper.currentApplication().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current =
            abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
        var voltage = 0.0
        kotlin.runCatching {
            val voltageNow =
                BufferedReader(FileReader("/sys/class/power_supply/battery/voltage_now"))
            voltage =
                BigDecimal(voltageNow.readLine().toDouble() / 1000.0).setScale(1, RoundingMode.HALF_UP).toDouble()
        }
        val powerAll = abs((current * voltage) / 1000f / 1000f)
        val power = String.format("%.2f", powerAll)

        // 电流/电压展示逻辑设置
        val mCurrent = when(mPrefsMap.getBoolean("system_ui_show_charging_c_more")) {
            true -> "$current mA"
            else -> "${String.format("%.1f", abs(current / 1000f))} A"
        }
        val mVoltage = when(mPrefsMap.getBoolean("system_ui_show_charging_v_more")) {
            true -> "${voltage.toInt()} mV"
            else -> "${String.format("%.1f", abs(voltage / 1000f))} V"
        }

        return "$mCurrent · $mVoltage · $power W"
    }

}