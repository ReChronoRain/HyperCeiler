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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.text.TextUtils
import android.widget.TextView
import androidx.annotation.RequiresPermission
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.getModuleRes
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.net.NetworkInterface
import kotlin.math.roundToLong

object NewNetworkSpeed : BaseHook() {
    private var measureTimeNanos: Long = 0L

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

    private val needTotal by lazy {
        (networkStyle == 1 || networkStyle == 2)
    }
    private val sharedStrArr = arrayOf("", "")
    @Volatile
    private var cachedUnits: CharArray? = null
    @Volatile
    private var cachedUnitSuffix: String? = null
    private const val KB = 1024.0
    private const val MB = KB * KB

    private val nscCls by lazy {
        loadClassOrNull("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader)
    }
    private val nsvCls by lazy {
        loadClassOrNull("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader)
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun init() {
        runCatching {
            if (isMoreAndroidVersion(36) && networkStyle != 0) {
                // 仅 Android 16 出现末尾加空格的情况
                nsvCls!!.methodFinder().filterByName("updateNetworkSpeed").first().createAfterHook {
                    val mNetworkSpeedNumberText = it.thisObject.getObjectFieldAs<TextView>("mNetworkSpeedNumberText")
                    val mNetworkSpeedNumber = it.thisObject.getObjectFieldAs<CharSequence>("mNetworkSpeedNumber")
                    if (!TextUtils.equals(mNetworkSpeedNumber, mNetworkSpeedNumberText.text)) {
                        mNetworkSpeedNumberText.text = mNetworkSpeedNumber
                    }
                }
            }
            nscCls!!.methodFinder().filterByName("updateText").filterByParamCount(1).first().createBeforeHook {
                // 获取该方法中的 Context
                val mContext = it.thisObject.getObjectField("mContext") as Context

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

                val strArr = sharedStrArr
                // 设置 tx/rx 字符串
                val txStr = if (hideLow && !allHideLow && txLow) "" else {
                    if (swapPlaces) "$txArrow${humanReadableByteCount(mContext, txSpeed)}"
                    else "${humanReadableByteCount(mContext, txSpeed)}$txArrow"
                }
                val rxStr = if (hideLow && !allHideLow && rxLow) "" else {
                    if (swapPlaces) "$rxArrow${humanReadableByteCount(mContext, rxSpeed)}"
                    else "${humanReadableByteCount(mContext, rxSpeed)}$rxArrow"
                }
                val totalStr = if (needTotal) humanReadableByteCount(mContext, txSpeed + rxSpeed) else ""

                // 存储隐藏慢速判断结果
                val isLowSpeed = hideLow && (txSpeed + rxSpeed) < lowLevel
                val isAllLowSpeed = hideLow && allHideLow && txLow && rxLow

                when (networkStyle) {
                    1, 2 -> {
                        // 单/双排显示总速率
                        if (isLowSpeed) {
                            strArr[0] = ""
                        } else {
                            strArr[0] = totalStr
                        }
                        it.args[0] = strArr
                    }
                    3 -> {
                        // 同一行显示上/下行
                        if (isAllLowSpeed) {
                            strArr[0] = ""
                        } else {
                            strArr[0] = if (rxStr.isNotEmpty()) "$txStr $rxStr" else txStr
                        }
                        it.args[0] = strArr
                    }
                    4 -> {
                        // 上下两行显示
                        if (isAllLowSpeed) {
                            strArr[0] = ""
                        } else {
                            strArr[0] = "$txStr\n$rxStr"
                        }
                        it.args[0] = strArr
                    }
                    else -> {
                        if (isLowSpeed) {
                            strArr[0] = ""
                            it.args[0] = strArr
                        }
                    }
                }
            }
        }.onFailure { e ->
            logE(TAG, lpparam.packageName, "hook failed by network speed: ${e.message}", e)
        }
    }

    private fun getTrafficBytes(out: LongArray) {
        out[0] = 0L // tx
        out[1] = 0L // rx

        runCatching {
            val list = NetworkInterface.getNetworkInterfaces()
            list?.asSequence()
                ?.filter { it.isUp && !it.isVirtual && !it.isLoopback && !it.isPointToPoint && it.name.isNotEmpty() }
                ?.forEach { iFace ->
                    TrafficStats::class.java.apply {
                        out[0] += callStaticMethod("getTxBytes", iFace.name) as Long
                        out[1] += callStaticMethod("getRxBytes", iFace.name) as Long
                    }
                }
        }.onFailure { t ->
            logE(TAG, this.lpparam.packageName, t)
            out[0] = TrafficStats.getTotalTxBytes()
            out[1] = TrafficStats.getTotalRxBytes()
        }
    }

    //  网速计算与隐藏相关
    @SuppressLint("DefaultLocale")
    private fun humanReadableByteCount(ctx: Context, bytes: Long): String {
        try {
            // 缓存模块 resources/units/suffix 以提高性能
            if (cachedUnits == null) {
                val modRes = getModuleRes(ctx)
                cachedUnitSuffix = if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit")) "" else modRes.getString(R.string.system_ui_statusbar_network_speed_Bs)
                val unitsStr = modRes.getString(R.string.system_ui_statusbar_network_speed_speedunits)
                cachedUnits = when {
                    unitsStr.isNotEmpty() -> unitsStr.toCharArray()
                    else -> charArrayOf('K', 'M')
                }
            }

            val units = cachedUnits!!
            val unitSuffix = cachedUnitSuffix ?: ""

            val value: Double
            val expIndex: Int

            if (bytes >= MB) {
                value = bytes / MB
                expIndex = 1
            } else {
                value = bytes / KB
                expIndex = 0
            }

            val valueStr = if (value < 100.0f) String.format("%.1f", value) else String.format("%.0f", value)
            val pre = units.getOrElse(expIndex) { if (expIndex == 1) 'M' else 'K' }
            return if (networkStyle == 2) {
                "$valueStr\n${pre}$unitSuffix"
            } else {
                "$valueStr${pre}$unitSuffix"
            }
        } catch (t: Throwable) {
            logE(TAG, this.lpparam.packageName, t)
            return ""
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun updateNetworkSpeed(mContext: Context) {
        val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isConnected = capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } ?: false

        if (!isConnected) {
            // 如果已为 0，就不用做任何事情，避免不必要的计算
            if (txSpeed != 0L || rxSpeed != 0L) {
                txSpeed = 0L
                rxSpeed = 0L
            }
            // 不改变 measureTimeNanos 或 bytesTotal，让恢复连接后第一帧跳过速度计算（避免巨大速度）
            return
        }

        val nowNanos = System.nanoTime()
        var interval = nowNanos - measureTimeNanos

        // 首次运行或异常情况下初始化时间点
        if (measureTimeNanos == 0L || interval <= 0L) {
            measureTimeNanos = nowNanos
            // initialize totals but do not compute speeds this tick
            val out = LongArray(2)
            getTrafficBytes(out)
            txBytesTotal = out[0]
            rxBytesTotal = out[1]
            txSpeed = 0L
            rxSpeed = 0L
            return
        }

        // 限制极短的时间间隔, 150ms 可以避免连续 UI 回调带来的抖动与高开销
        if (interval < 150_000_000L) {
            // 仅更新时间测点，不计算速度
            measureTimeNanos = nowNanos
            return
        }
        // 限制过长的时间间隔，避免异常情况导致的巨大速度
        if (interval > 10_000_000_000L) {
            interval = 10_000_000_000L
        }
        measureTimeNanos = nowNanos

        val out = LongArray(2)
        getTrafficBytes(out)
        val newTxBytes = out[0]
        val newRxBytes = out[1]

        if (txBytesTotal != 0L || rxBytesTotal != 0L) {
            newTxBytesFixed = (newTxBytes - txBytesTotal).takeIf { it >= 0 } ?: 0
            newRxBytesFixed = (newRxBytes - rxBytesTotal).takeIf { it >= 0 } ?: 0

            val seconds = interval / 1_000_000_000.0
            txSpeed = (newTxBytesFixed / seconds).roundToLong()
            rxSpeed = (newRxBytesFixed / seconds).roundToLong()
        } else {
            // 首次读取之后不计算速度（上面的分支已经在首次中止），这里只是安全赋值
            txSpeed = 0L
            rxSpeed = 0L
        }

        txBytesTotal = newTxBytes
        rxBytesTotal = newRxBytes
    }
}
