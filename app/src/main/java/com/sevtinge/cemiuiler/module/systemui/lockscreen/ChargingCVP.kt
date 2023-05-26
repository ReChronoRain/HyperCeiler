package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.os.BatteryManager
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object ChargingCVP : BaseHook() {

    @SuppressLint("SetTextI18n")
    override fun init() {

        findMethod("com.android.keyguard.charge.ChargeUtils") {
            name == "getChargingHintText" && parameterCount == 3
        }.hookAfter {
            if (it.result != null) {
                it.result = it.result as String + "\n" + getCVP()
            }
        }

        findClassIfExists("com.android.systemui.statusbar.phone.KeyguardIndicationTextView").hookAllConstructorAfter {
            (it.thisObject as TextView).isSingleLine = false
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
                BigDecimal(voltageNow.readLine().toDouble() / 1000000.0).setScale(1, RoundingMode.HALF_UP).toDouble()
        }
        val powerAll = abs((current * voltage) / 1000f)
        val power = String.format("%.2f", powerAll)
        return "$current mA · $voltage V · $power W"
    }

}