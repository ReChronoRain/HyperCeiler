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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.PowerManager
import android.util.ArrayMap
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getStaticObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.invokeStaticMethodBestMatch
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadFirstClass
import io.github.kyuubiran.ezxhelper.core.util.ObjectUtil.invokeMethodBestMatch
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object ChargingCVP : BaseHook() {
    private val showSpacingValue by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_show_spacing_value")
    }
    private val isShowTemp by lazy {
        mPrefsMap.getBoolean("system_ui_show_battery_temperature")
    }
    private val isShowMoreC by lazy {
        mPrefsMap.getBoolean("system_ui_show_charging_c_more")
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        // 去除单行限制
        val clazzDependency = loadClass("com.android.systemui.Dependency")
        val clazzKeyguardIndicationController =
            loadClass("com.android.systemui.statusbar.KeyguardIndicationController")

        loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.createHooks {
            after { param ->
                (param.thisObject as TextView).apply {
                    isSingleLine = false
                    textSize = 8.2f
                }
                if (showSpacingValue) {
                    // 是否更改刷新频率
                    setShowSpacing(clazzDependency, clazzKeyguardIndicationController, param)
                }
            }
        }


        // 修改底部文本信息
        loadFirstClass(
            "com.miui.charge.ChargeUtils", "com.android.keyguard.charge.ChargeUtils"
        ).methodFinder().filterByName("getChargingHintText").filterByParamCount(3).first()
            .createHook {
                after { param ->
                    param.result = param.result?.let {
                        "${getTemp()}$it\n${getCVP()}"
                    }
                }
            }

    }

    private fun setShowSpacing(
        clazzDependency: Class<*>,
        clazzKeyguardIndicationController: Class<*>,
        param: XC_MethodHook.MethodHookParam
    ) {
        val screenOnOffReceiver = object : BroadcastReceiver() {
            val keyguardIndicationController = runCatching {
                invokeStaticMethodBestMatch(clazzDependency, "get", null, clazzKeyguardIndicationController)!!
            }.getOrElse {
                val clazzMiuiStub = loadClass("miui.stub.MiuiStub")
                val instanceMiuiStub =
                    clazzMiuiStub.getStaticObjectFieldOrNull("INSTANCE")!!
                val mSysUIProvider =
                    instanceMiuiStub.getObjectFieldOrNull("mSysUIProvider")!!
                val mKeyguardIndicationController =
                    mSysUIProvider.getObjectFieldOrNull("mKeyguardIndicationController")!!
                invokeMethodBestMatch(mKeyguardIndicationController, "get")!!
            }
            val handler = Handler((param.thisObject as TextView).context.mainLooper)
            val runnable = object : Runnable {
                val clazzMiuiDependency =
                    loadClass("com.miui.systemui.MiuiDependency")
                val clazzMiuiChargeController =
                    loadClass("com.miui.charge.MiuiChargeController")
                val sDependency =
                    clazzMiuiDependency.getStaticObjectFieldOrNull("sDependency")!!
                val mProviders =
                    sDependency.getObjectFieldOrNull("mProviders") as ArrayMap<*, *>
                val mMiuiChargeControllerProvider = mProviders[clazzMiuiChargeController]!!
                val instanceMiuiChargeController = if (isMoreHyperOSVersion(2f) && isMoreAndroidVersion(35)) {
                    mMiuiChargeControllerProvider
                        .getObjectFieldOrNull("f$0")!!
                        .callMethod("get")!!
                } else {
                    invokeMethodBestMatch(
                        mMiuiChargeControllerProvider, "createDependency"
                    )!!
                }

                override fun run() {
                    doUpdateForHyperOS()
                    handler.postDelayed(
                        this,
                        mPrefsMap.getInt("system_ui_statusbar_lock_screen_show_spacing", 6) / 2 * 1000L
                    )
                }

                fun doUpdateForHyperOS() {
                    val mBatteryStatus =
                        instanceMiuiChargeController.getObjectFieldOrNull("mBatteryStatus")!!
                    val level = mBatteryStatus.getObjectFieldOrNull("level")
                    val plugged = mBatteryStatus.getObjectFieldOrNull("plugged") as Int
                    val isPluggedIn = if (isMoreHyperOSVersion(2f) && isMoreAndroidVersion(35)) {
                        mBatteryStatus.callMethod("isPluggedIn", plugged)
                    } else {
                        invokeMethodBestMatch(mBatteryStatus, "isPluggedIn")
                    }
                    val mContext =
                        instanceMiuiChargeController.getObjectFieldOrNull("mContext")
                    val clazzChargeUtils =
                        loadClass("com.miui.charge.ChargeUtils", lpparam.classLoader)
                    val chargingHintText =
                        invokeStaticMethodBestMatch(
                            clazzChargeUtils,
                            "getChargingHintText",
                            null,
                            level,
                            isPluggedIn,
                            mContext
                        )
                    keyguardIndicationController.setObjectField("mComputePowerIndication", chargingHintText)
                    invokeMethodBestMatch(
                        keyguardIndicationController,
                        "updateDeviceEntryIndication",
                        null,
                        false
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
        (param.thisObject as TextView).context.registerReceiver(
            screenOnOffReceiver, filter
        )
    }

    private fun getTemp(): String {
        var temp = 0.0

        try {
            BufferedReader(FileReader("/sys/class/power_supply/battery/temp")).use { reader ->
                temp = BigDecimal(reader.readLine().toDouble() / 10.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .toDouble()
            }
        } catch (_: Exception) {
            temp = 0.0
        }

        return if (isShowTemp) "$temp ℃ · " else ""
    }

    @SuppressLint("DefaultLocale")
    private fun getCVP(): String {
        runCatching {
            var current = 0.0
            var voltage = 0.0
            val watt: Double by lazy {
                current * voltage
            }

            current = FileReader("/sys/class/power_supply/battery/current_now").use { fileReader ->
                BufferedReader(fileReader).use { bufferedReader ->
                    -1.0 * bufferedReader.readLine().toDouble() / 1000000.0
                }
            }
            voltage = FileReader("/sys/class/power_supply/battery/voltage_now").use { fileReader ->
                BufferedReader(fileReader).use { bufferedReader ->
                    bufferedReader.readLine().toDouble() / 1000000.0
                }
            }

            // 计算功率信息
            val power = String.format("%.2f", watt)

            // 电流展示逻辑设置
            val mCurrent = if (isShowMoreC) {
                "${(current * 1000).toInt()} mA"
            } else {
                "${String.format("%.1f", abs(current))} A"
            }
            val mVoltage = "${String.format("%.1f", voltage)} V"

            // 判断充满信息是否归零
            val showBattery = if (current == 0.0) {
                ""
            } else {
                "$mCurrent · $mVoltage · $power W"
            }

            // 输出展示信息
            return showBattery
        }
        return ""
    }
}
