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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old

import android.annotation.*
import android.content.*
import android.net.*
import android.util.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.*
import de.robv.android.xposed.*
import java.net.*
import kotlin.math.*

object NetworkSpeed : BaseHook() {
    private var measureTime: Long = 0
    private var newTxBytesFixed: Long = 0
    private var newRxBytesFixed: Long = 0
    private var txBytesTotal: Long = 0
    private var rxBytesTotal: Long = 0
    private var txSpeed: Long = 0
    private var rxSpeed: Long = 0

    //  隐藏慢速
    private val hideLow by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide")
    }

    // 网速均低于设定值隐藏
    private val allHideLow by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide_all")
    }

    //  慢速水平
    private val lowLevel by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_hide_slow", 1) * 1024
    }

    // 交换图标与网速位置
    private val swapPlaces by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_swap_places")
    }

    // 网速图标
    private val icons by lazy {
        mPrefsMap.getString("system_ui_statusbar_network_speed_icon", "2").toInt()
    }

    // 网速指示器样式
    private val networkStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_style", 0)
    }

    private fun getTrafficBytes(): Pair<Long, Long> {
        var tx = -1L
        var rx = -1L
        try {
            val list = NetworkInterface.getNetworkInterfaces()
            while (list.hasMoreElements()) {
                val iFace = list.nextElement()
                if (iFace.isUp && !iFace.isVirtual && !iFace.isLoopback && !iFace.isPointToPoint && "" != iFace.name) {
                    tx +=
                        XposedHelpers.callStaticMethod(TrafficStats::class.java, "getTxBytes", iFace.name) as Long
                    rx +=
                        XposedHelpers.callStaticMethod(TrafficStats::class.java, "getRxBytes", iFace.name) as Long
                }
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
            tx = TrafficStats.getTotalTxBytes()
            rx = TrafficStats.getTotalRxBytes()
        }
        return Pair(tx, rx)
    }

    //  网速计算与隐藏相关
    @SuppressLint("DefaultLocale")
    private fun humanReadableByteCount(ctx: Context, bytes: Long): String {
        return try {
            val modRes = getModuleRes(ctx)
            val hideSecUnit = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit")
            var unitSuffix = modRes.getString(R.string.system_ui_statusbar_network_speed_Bs)
            if (hideSecUnit) unitSuffix = ""
            var f = bytes / 1024.0f
            var expIndex = 0
            if (f > 999.0f) {
                expIndex = 1
                f /= 1024.0f
            }
            val pre = modRes.getString(R.string.system_ui_statusbar_network_speed_speedunits)[expIndex]
            if (networkStyle == 2) {
                (if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)) + "\n" + String.format("%s$unitSuffix", pre)
            } else {
                (if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)) + String.format("%s$unitSuffix", pre)
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
            ""
        }
    }

    override fun init() {
        // 双排网速相关
        val nscCls by lazy {
            findClassIfExists("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader)
        }

        if (nscCls == null) {
            logE(
                TAG,
                this.lpparam.packageName,
                "DetailedNetSpeedHook: No NetworkSpeed view or controller"
            )
        } else {
            nscCls.methodFinder()
                .filterByName("getTotalByte")
                .single().createHook {
                    after {
                        val bytes = getTrafficBytes()
                        txBytesTotal = bytes.first
                        rxBytesTotal = bytes.second
                        measureTime = System.nanoTime()
                    }
                }

            nscCls.methodFinder()
                .filterByName("updateNetworkSpeed")
                .single().createHook {
                    before {
                        var isConnected = false
                        val mContext =
                            XposedHelpers.getObjectField(it.thisObject, "mContext") as Context
                        val mConnectivityManager =
                            mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val nw = mConnectivityManager.activeNetwork
                        if (nw != null) {
                            val capabilities = mConnectivityManager.getNetworkCapabilities(nw)
                            if (capabilities != null && (!(!
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                                    !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
                            ) {
                                isConnected = true
                            }
                        }
                        if (isConnected) {
                            val nanoTime = System.nanoTime()
                            var newTime = nanoTime - measureTime
                            measureTime = nanoTime
                            if (newTime == 0L) newTime = (4 * 10.0.pow(9.0)).roundToLong()
                            val bytes = getTrafficBytes()
                            val newTxBytes = bytes.first
                            val newRxBytes = bytes.second
                            newTxBytesFixed = newTxBytes - txBytesTotal
                            newRxBytesFixed = newRxBytes - rxBytesTotal
                            if (newTxBytesFixed < 0 || txBytesTotal == 0L) newTxBytesFixed = 0
                            if (newRxBytesFixed < 0 || rxBytesTotal == 0L) newRxBytesFixed = 0
                            txSpeed = (newTxBytesFixed / (newTime / 10.0.pow(9.0))).roundToLong()
                            rxSpeed = (newRxBytesFixed / (newTime / 10.0.pow(9.0))).roundToLong()
                            txBytesTotal = newTxBytes
                            rxBytesTotal = newRxBytes
                        } else {
                            txSpeed = 0
                            rxSpeed = 0
                        }
                    }
                }

            nscCls.methodFinder().filterByName("formatSpeed").filterByParamCount(2).single()
                .createHook {
                    before {
                        // 计算网速方向的箭头
                        val txArrow = when (icons) {
                            2 -> if (txSpeed < lowLevel) "△" else "▲"
                            3 -> if (txSpeed < lowLevel) " ▵" else " ▴"
                            4 -> if (txSpeed < lowLevel) " ☖" else " ☗"
                            5 -> if (txSpeed < lowLevel) "↑" else "↑"
                            6 -> if (txSpeed < lowLevel) "⇧" else "⇧"
                            else -> ""
                        }

                        val rxArrow = when (icons) {
                            2 -> if (rxSpeed < lowLevel) "▽" else "▼"
                            3 -> if (rxSpeed < lowLevel) " ▿" else " ▾"
                            4 -> if (rxSpeed < lowLevel) " ⛉" else " ⛊"
                            5 -> if (rxSpeed < lowLevel) "↓" else "↓"
                            6 -> if (rxSpeed < lowLevel) "⇩" else "⇩"
                            else -> ""
                        }

                        // 计算上行网速
                        val tx = if (hideLow && !allHideLow && txSpeed < lowLevel) ""
                        else if (swapPlaces) "$txArrow${humanReadableByteCount(it.args[0] as Context, txSpeed)}"
                        else "${humanReadableByteCount(it.args[0] as Context, txSpeed)}$txArrow"

                        // 计算下行网速
                        val rx = if (hideLow && !allHideLow && rxSpeed < lowLevel) ""
                        else if (swapPlaces) "$rxArrow${humanReadableByteCount(it.args[0] as Context, rxSpeed)}"
                        else "${humanReadableByteCount(it.args[0] as Context, rxSpeed)}$rxArrow"

                        // 计算总网速
                        val ax = humanReadableByteCount(it.args[0] as Context, newTxBytesFixed + newRxBytesFixed)

                        // 是否隐藏慢速的判定
                        val isLowSpeed = hideLow && (txSpeed + rxSpeed) < lowLevel
                        val isAllLowSpeed = hideLow && allHideLow && txSpeed < lowLevel && rxSpeed < lowLevel

                        // 返回结果
                        it.result = when (networkStyle) {
                            1, 2 -> if (isLowSpeed) "" else ax
                            3 -> if (isAllLowSpeed) "" else "$tx $rx"
                            4 -> if (isAllLowSpeed) "" else "$tx\n$rx"
                            else -> if (isLowSpeed) "" else it.result as String
                        }
                    }
                }
        }
    }
}
