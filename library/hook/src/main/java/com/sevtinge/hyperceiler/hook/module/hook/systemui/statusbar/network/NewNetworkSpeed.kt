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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Pair
import androidx.annotation.RequiresPermission
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.getModuleRes
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import de.robv.android.xposed.XposedHelpers
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.net.NetworkInterface
import kotlin.math.pow
import kotlin.math.roundToLong

object NewNetworkSpeed : BaseHook() {
    private var measureTime: Long = 0

    private var newTxBytesFixed: Long = 0
    private var newRxBytesFixed: Long = 0

    private var txBytesTotal: Long = 0
    private var rxBytesTotal: Long = 0

    private var txSpeed: Long = 0
    private var rxSpeed: Long = 0

    private var txArrow = ""
    private var rxArrow = ""

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
        runCatching {
            val list = NetworkInterface.getNetworkInterfaces()
            list?.asSequence()
                ?.filter { it.isUp && !it.isVirtual && !it.isLoopback && !it.isPointToPoint && it.name.isNotEmpty() }
                ?.forEach { iFace ->
                    tx += XposedHelpers.callStaticMethod(TrafficStats::class.java, "getTxBytes", iFace.name) as Long
                    rx += XposedHelpers.callStaticMethod(TrafficStats::class.java, "getRxBytes", iFace.name) as Long
                }
        }.onFailure { t ->
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
            val unitSuffix = if (hideSecUnit) "" else modRes.getString(R.string.system_ui_statusbar_network_speed_Bs)
            var f = bytes / 1024.0f
            val expIndex = if (f > 999.0f) {
                f /= 1024.0f
                1
            } else 0
            val pre = modRes.getString(R.string.system_ui_statusbar_network_speed_speedunits)[expIndex]
            val valueStr = if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)
            if (networkStyle == 2) {
                "$valueStr\n${pre}$unitSuffix"
            } else {
                "$valueStr${pre}$unitSuffix"
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
            ""
        }
    }

    private val nscCls by lazy {
        findClassIfExists("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader)
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun init() {
        if (nscCls == null) {
            logE(TAG, this.lpparam.packageName, "DetailedNetSpeedHook: No NetworkSpeed view or controller")
        } else {
            nscCls.methodFinder().filterByName("updateText").filterByParamCount(1).first().createBeforeHook {
                // 初始化字符串数组和获取该方法中的 Context
                val strArr = arrayOfNulls<String>(2)
                val mContext =
                    it.thisObject.getObjectField("mContext") as Context

                // 更新网速
                updateNetworkSpeed(mContext)
                // 设置上下行网速图标
                val txLow = txSpeed < lowLevel
                val rxLow = rxSpeed < lowLevel
                txArrow = when (icons) {
                    2 -> if (txLow) "△" else "▲"
                    3 -> if (txLow) " ▵" else " ▴"
                    4 -> if (txLow) " ☖" else " ☗"
                    5 -> "↑"
                    6 -> "⇧"
                    else -> ""
                }
                rxArrow = when (icons) {
                    2 -> if (rxLow) "▽" else "▼"
                    3 -> if (rxLow) " ▿" else " ▾"
                    4 -> if (rxLow) " ⛉" else " ⛊"
                    5 -> "↓"
                    6 -> "⇩"
                    else -> ""
                }

                // 计算上/下行网速字符串
                val tx = if (hideLow && !allHideLow && txLow) "" else {
                    if (swapPlaces) "$txArrow${humanReadableByteCount(mContext, txSpeed)}"
                    else "${humanReadableByteCount(mContext, txSpeed)}$txArrow"
                }
                val rx = if (hideLow && !allHideLow && rxLow) "" else {
                    if (swapPlaces) "$rxArrow${humanReadableByteCount(mContext, rxSpeed)}"
                    else "${humanReadableByteCount(mContext, rxSpeed)}$rxArrow"
                }
                // 计算总网速
                val ax = humanReadableByteCount(mContext, txSpeed + rxSpeed)
                // 存储是否隐藏慢速的条件的结果
                val isLowSpeed = hideLow && (txSpeed + rxSpeed) < lowLevel
                val isAllLowSpeed = hideLow && allHideLow && txLow && rxLow

                runCatching {
                    when (networkStyle) {
                        // 如果开启值和单位单双排显示，返回总网速的字符串
                        1, 2 -> {
                            if (isLowSpeed) {
                                strArr[0] = ""
                                strArr[1] = ""
                            } else {
                                strArr[0] = ax
                                strArr[1] = ""
                            }
                            it.args[0] = strArr
                        }
                        // 如果显示上下行网速显示，返回上下行网速的字符串
                        3 -> {
                            if (isAllLowSpeed) {
                                strArr[0] = ""
                                strArr[1] = ""
                            } else {
                                strArr[0] = "$tx $rx"
                                strArr[1] = ""
                            }
                            it.args[0] = strArr
                        }
                        4 -> {
                            if (isAllLowSpeed) {
                                strArr[0] = ""
                                strArr[1] = ""
                            } else {
                                strArr[0] = "$tx\n$rx"
                                strArr[1] = ""
                            }
                            it.args[0] = strArr
                        }
                        // 其他情况，对隐藏慢速判定，返回空字符串，其余不返回
                        else -> {
                            if (isLowSpeed) {
                                strArr[0] = ""
                                strArr[1] = ""
                                it.args[0] = strArr
                            }
                        }
                    }
                }.onFailure { e ->
                    logE(TAG, lpparam.packageName, "NetSpeedHook: hook failed by network speed, ${e.printStackTrace()}")
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun updateNetworkSpeed(mContext: Context) {
        var isConnected = false
        val mConnectivityManager =
            mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // 通过 ConnectivityManager 获取当前活跃的网络对象
        val nw = mConnectivityManager.activeNetwork
        // 如果 nw 不为空，说明有网络连接
        if (nw != null) {
            // 获取网络的能力对象
            val capabilities = mConnectivityManager.getNetworkCapabilities(nw)
            // 如果能力对象不为空，并且支持无线或者蜂窝网络，那么将 isConnected 设为 true
            if (capabilities != null && (!(!
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
            ) {
                isConnected = true
            }
        }
        // 如果 isConnected 为 true，说明网络已连接
        if (isConnected) {
            // 获取系统的纳秒时间
            val nanoTime = System.nanoTime()
            // 计算两次获取时间的差值
            var newTime = nanoTime - measureTime
            measureTime = nanoTime
            // 如果 newTime 为 0，说明时间间隔太短，那么将 newTime 设为 4 秒
            if (newTime == 0L) newTime = (4 * 10.0.pow(9.0)).roundToLong()
            // 调用一个函数，返回一个包含两个元素的列表，第一个元素是发送的字节数，第二个元素是接收的字节数
            val bytes = getTrafficBytes()
            val newTxBytes = bytes.first
            val newRxBytes = bytes.second
            // 计算两次获取字节数的差值
            newTxBytesFixed = newTxBytes - txBytesTotal
            newRxBytesFixed = newRxBytes - rxBytesTotal
            // 如果 newTxBytesFixed 小于 0 或者 txBytesTotal 等于 0，说明发送的字节数有误，那么将 newTxBytesFixed 设为 0
            if (newTxBytesFixed < 0 || txBytesTotal == 0L) newTxBytesFixed = 0
            // 如果 newRxBytesFixed 小于 0 或者 rxBytesTotal 等于 0，说明接收的字节数有误，那么将 newRxBytesFixed 设为 0
            if (newRxBytesFixed < 0 || rxBytesTotal == 0L) newRxBytesFixed = 0
            // 计算发送和接收的速度，单位是字节每秒，用差值除以时间再取整数
            txSpeed = (newTxBytesFixed / (newTime / 10.0.pow(9.0))).roundToLong()
            rxSpeed = (newRxBytesFixed / (newTime / 10.0.pow(9.0))).roundToLong()
            // 更新总的发送和接收的字节数
            txBytesTotal = newTxBytes
            rxBytesTotal = newRxBytes
        } else {
            // 如果 isConnected 为 false，说明网络未连接，那么将发送和接收的速度设为 0
            txSpeed = 0
            rxSpeed = 0
        }
    }
 }
