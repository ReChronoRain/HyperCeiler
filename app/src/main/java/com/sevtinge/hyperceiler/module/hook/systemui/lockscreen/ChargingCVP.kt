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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.annotation.*
import android.app.*
import android.content.*
import android.os.*
import android.util.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNull
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadFirstClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*
import java.io.*
import java.math.*
import kotlin.math.*

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
                (param.thisObject as TextView).let {
                    it.isSingleLine = false
                    it.textSize = 8.2f
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
                invokeStaticMethodBestMatch(
                    clazzDependency, "get", null, clazzKeyguardIndicationController
                )!!
            }.getOrElse {
                val clazzMiuiStub = loadClass("miui.stub.MiuiStub")
                val instanceMiuiStub =
                    getStaticObjectOrNull(clazzMiuiStub, "INSTANCE")!!
                val mSysUIProvider =
                    getObjectOrNull(instanceMiuiStub, "mSysUIProvider")!!
                val mKeyguardIndicationController =
                    getObjectOrNull(
                        mSysUIProvider,
                        "mKeyguardIndicationController"
                    )!!
                invokeMethodBestMatch(mKeyguardIndicationController, "get")!!
            }
            val handler = Handler((param.thisObject as TextView).context.mainLooper)
            val runnable = object : Runnable {
                val clazzMiuiDependency =
                    loadClass("com.miui.systemui.MiuiDependency")
                val clazzMiuiChargeController =
                    loadClass("com.miui.charge.MiuiChargeController")
                val sDependency =
                    getStaticObjectOrNull(clazzMiuiDependency, "sDependency")!!
                val mProviders =
                    getObjectOrNull(sDependency, "mProviders") as ArrayMap<*, *>
                val mMiuiChargeControllerProvider =
                    mProviders[clazzMiuiChargeController]!!
                val instanceMiuiChargeController = if (isMoreHyperOSVersion(2f)) {
                    mMiuiChargeControllerProvider
                        .getObjectFieldOrNull("f$0")!!
                        .callMethod("get")!!
                } else {
                    invokeMethodBestMatch(
                        mMiuiChargeControllerProvider, "createDependency"
                    )!!
                }

                override fun run() {
                    if (isMoreHyperOSVersion(1f)) {
                        doUpdateForHyperOS()
                    } else if (!isMoreHyperOSVersion(1f) && isAndroidVersion(34)) {
                        XposedHelpers.callStaticMethod(
                            loadClass("com.android.systemui.statusbar.KeyguardIndicationController"),
                            "updatePowerIndication"
                        )
                    } else {
                        invokeMethodBestMatch(
                            keyguardIndicationController,
                            "updatePowerIndication"
                        )
                    }
                    handler.postDelayed(
                        this,
                        mPrefsMap.getInt(
                            "system_ui_statusbar_lock_screen_show_spacing",
                            6
                        ) / 2 * 1000L
                    )
                }

                fun doUpdateForHyperOS() {
                    val mBatteryStatus =
                        getObjectOrNull(instanceMiuiChargeController, "mBatteryStatus")!!
                    val level = getObjectOrNull(mBatteryStatus, "level")
                    val plugged = getObjectOrNull(mBatteryStatus, "plugged") as Int
                    val isPluggedIn = if (isMoreHyperOSVersion(2f)) {
                        mBatteryStatus.callMethod("isPluggedIn", plugged)
                    } else {
                        invokeMethodBestMatch(mBatteryStatus, "isPluggedIn")
                    }
                    val mContext =
                        getObjectOrNull(instanceMiuiChargeController, "mContext")
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
                    setObject(
                        keyguardIndicationController,
                        "mComputePowerIndication",
                        chargingHintText
                    )
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

        runCatching {
            // 获取电池温度信息
            val temNow =
                BufferedReader(FileReader("/sys/class/power_supply/battery/temp"))
            temp =
                BigDecimal(temNow.readLine().toDouble() / 10.0).setScale(1, RoundingMode.HALF_UP).toDouble()
        }

        // 电池温度是否展示
        val mTemp = if (isShowTemp) "$temp ℃ · " else ""

        return mTemp
    }

    @SuppressLint("DefaultLocale")
    private fun getCVP(): String {
        // 获取电流信息
        val batteryManager =
            AndroidAppHelper.currentApplication()
                .getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current =
            abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
        var voltage = 0.0

        runCatching {
            // 获取电压信息
            val voltageNow =
                BufferedReader(FileReader("/sys/class/power_supply/battery/voltage_now"))
            voltage =
                BigDecimal(voltageNow.readLine().toDouble() / 1000.0).setScale(1, RoundingMode.HALF_UP).toDouble()
        }

        // 计算功率信息
        val powerAll = abs((current * voltage) / 1000f / 1000f)
        val power = String.format("%.2f", powerAll)

        // 电流展示逻辑设置
        val mCurrent = if (isShowMoreC) {
            "$current mA"
        } else {
            "${String.format("%.1f", abs(current / 1000f))} A"
        }
        val mVoltage = "${String.format("%.1f", abs(voltage / 1000f))} V"

        // 判断充满信息是否归零
        val showBattery = if (current == 0) {
            ""
        } else {
            "$mCurrent · $mVoltage · $power W"
        }

        // 输出展示信息
        return showBattery
    }
}
