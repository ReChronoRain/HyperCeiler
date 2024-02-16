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
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.util.ArrayMap
import android.widget.TextView
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
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.IS_HYPER_OS
import com.sevtinge.hyperceiler.utils.devicesdk.getAndroidVersion
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import de.robv.android.xposed.XposedHelpers
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object ChargingCVP : BaseHook() {
    private val showSpacingValue by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_show_spacing_value")
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        // 去除单行限制，Android13 以上扩展一个刷新频率
        if (getAndroidVersion() >= 33) {
            val clazzDependency = loadClass("com.android.systemui.Dependency")
            val clazzKeyguardIndicationController =
                loadClass("com.android.systemui.statusbar.KeyguardIndicationController")
            loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.createHooks {
                after { param ->
                    (param.thisObject as TextView).isSingleLine = false
                    if (showSpacingValue) {
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
                                val instanceMiuiChargeController = invokeMethodBestMatch(
                                    mMiuiChargeControllerProvider, "createDependency"
                                )!!

                                override fun run() {
                                    if (IS_HYPER_OS) {
                                        doUpdateForHyperOS()
                                    } else if (!IS_HYPER_OS && isAndroidVersion(34)) {
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
                                    val mBatteryStatus = getObjectOrNull(
                                        instanceMiuiChargeController, "mBatteryStatus"
                                    )!!
                                    val level = getObjectOrNull(mBatteryStatus, "level")
                                    val isPluggedIn =
                                        invokeMethodBestMatch(mBatteryStatus, "isPluggedIn")
                                    val mContext =
                                        getObjectOrNull(instanceMiuiChargeController, "mContext")
                                    val clazzChargeUtils = loadClass("com.miui.charge.ChargeUtils", lpparam.classLoader)
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
                }
            }
        } else {
            loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")!!.constructors.createHooks {
                after {
                    (it.thisObject as TextView).isSingleLine = false
                }
            }
        }

        // 修改底部文本信息
        loadFirstClass(
            "com.miui.charge.ChargeUtils", "com.android.keyguard.charge.ChargeUtils"
        ).methodFinder().filterByName("getChargingHintText").filterByParamCount(3).first()
            .createHook {
                after { param ->
                    param.result = param.result?.let { "$it${getCVP()}" }
                }
            }

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
        val mCurrent = if (mPrefsMap.getBoolean("system_ui_show_charging_c_more")) {
            "$current mA"
        } else {
            "${String.format("%.1f", abs(current / 1000f))} A"
        }
        val mVoltage = "${String.format("%.1f", abs(voltage / 1000f))} V"
        // 电池温度是否展示
        val mTemp = if (mPrefsMap.getBoolean("system_ui_show_battery_temperature")) " · $temp ℃" else ""

        // 判断充满信息是否归零
        val showBattery = if (current == 0) {
            mTemp
        } else {
            "$mTemp\n$mCurrent · $mVoltage · $power W"
        }

        // 输出展示信息
        return showBattery
    }
}
