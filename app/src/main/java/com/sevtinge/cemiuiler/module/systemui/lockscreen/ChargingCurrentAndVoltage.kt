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
import java.io.File
import java.io.FileReader
import java.math.BigDecimal
import kotlin.math.abs

object ChargingCurrentAndVoltage : BaseHook() {

    @SuppressLint("SetTextI18n")
    override fun init() {

        findMethod("com.android.keyguard.charge.ChargeUtils") {
            name == "getChargingHintText" && parameterCount == 3
        }.hookAfter {
            it.result = it.result as String + "\n" + getCV()
        }

        findClassIfExists("com.android.systemui.statusbar.phone.KeyguardIndicationTextView").hookAllConstructorAfter {
            (it.thisObject as TextView).isSingleLine = false
        }
    }

    private fun getCV(): String {
        val batteryManager = AndroidAppHelper.currentApplication().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current = abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
        val wirelessSupport = File("/sys/class/power_supply/wireless/signal_strength").exists()
        val usb = BufferedReader(FileReader("/sys/class/power_supply/usb/voltage_now"))
        val wireless = BufferedReader(FileReader("/sys/class/power_supply/wireless/voltage_now"))
        val usbVoltage = BigDecimal(usb.readLine().toDouble() / 1000000.0).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
        val wirelessVoltage = if (wirelessSupport) {
            BigDecimal(wireless.readLine().toDouble() / 1000000.0).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
        } else 0.0
        val voltage = if (usbVoltage >= wirelessVoltage) usbVoltage else wirelessVoltage
        return "$current mA · $voltage V"
    }

}