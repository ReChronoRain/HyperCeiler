package com.sevtinge.cemiuiler.module.systemui.statusbar.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.util.Pair
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers
import java.net.NetworkInterface
import kotlin.math.pow
import kotlin.math.roundToLong

object NetworkSpeed : BaseHook() {
    private var measureTime: Long = 0
    private var newTxBytesFixed: Long = 0
    private var newRxBytesFixed: Long = 0
    private var txBytesTotal: Long = 0
    private var rxBytesTotal: Long = 0
    private var txSpeed: Long = 0
    private var rxSpeed: Long = 0

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
            logE(t)
            tx = TrafficStats.getTotalTxBytes()
            rx = TrafficStats.getTotalRxBytes()
        }
        return Pair(tx, rx)
    }

    //  网速计算与隐藏相关
    private fun humanReadableByteCount(ctx: Context, bytes: Long): String {
        return try {
            val modRes = Helpers.getModuleRes(ctx)
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
            if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_fakedualrow")) {
                (if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)) + "\n" + String.format("%s$unitSuffix", pre)
            } else {
                (if (f < 100.0f) String.format("%.1f", f) else String.format("%.0f", f)) + String.format("%s$unitSuffix", pre)
            }
        } catch (t: Throwable) {
            Helpers.log(t)
            ""
        }
    }

    override fun init() {
        // 双排网速相关
        val networkClass = when {
            Build.VERSION.SDK_INT == 30 -> "com.android.systemui.statusbar.NetworkSpeedController"
            Build.VERSION.SDK_INT >= 31 -> "com.android.systemui.statusbar.policy.NetworkSpeedController"
            else -> null
        }

        val nscCls =
            findClassIfExists(networkClass, lpparam.classLoader)

        if (nscCls == null) {
            Helpers.log("DetailedNetSpeedHook", "Cemiuiler: No NetworkSpeed view or controller")
        } else {
            nscCls.methodFinder().first {
                name == "getTotalByte"
            }.createHook {
                after {
                    val bytes = getTrafficBytes()
                    txBytesTotal = bytes.first
                    rxBytesTotal = bytes.second
                    measureTime = System.nanoTime()
                }
            }

            nscCls.methodFinder().first {
                name == "updateNetworkSpeed"
            }.createHook {
                before {
                    var isConnected = false
                    val mContext =
                        XposedHelpers.getObjectField(it.thisObject, "mContext") as Context
                    val mConnectivityManager =
                        mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val nw = mConnectivityManager.activeNetwork
                    if (nw != null) {
                        val capabilities = mConnectivityManager.getNetworkCapabilities(nw)
                        if (capabilities != null && (!(!capabilities.hasTransport
                                (NetworkCapabilities.TRANSPORT_WIFI) &&
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

            nscCls.methodFinder().filterByName("formatSpeed").filterByParamCount(2)
                .first()
                .createHook {
                    before {
                        //  隐藏慢速
                        val hideLow =
                            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide")
                        // 网速均低于设定值隐藏
                        val allHideLow =
                            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide_all")
                        //  慢速水平
                        val lowLevel =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_hide_slow", 1) * 1024
                        // 值和单位双排显示
                        val fakeDualRow =
                            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_fakedualrow")
                        // 上下行网速双排显示
                        val doubleUpDown =
                            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_show_up_down")
                        // 交换图标与网速位置
                        val swapPlaces =
                            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_swap_places")
                        // 网速图标
                        val icons =
                            mPrefsMap.getString("system_ui_statusbar_network_speed_icon", "2").toInt()
                        var txArrow = ""
                        var rxArrow = ""

                        when (icons) {
                            2 -> {
                                txArrow = if (txSpeed < lowLevel) "△" else "▲"
                                rxArrow = if (rxSpeed < lowLevel) "▽" else "▼"
                            }

                            3 -> {
                                txArrow = if (txSpeed < lowLevel) " ▵" else " ▴"
                                rxArrow = if (rxSpeed < lowLevel) " ▿" else " ▾"
                            }

                            4 -> {
                                txArrow = if (txSpeed < lowLevel) " ☖" else " ☗"
                                rxArrow = if (rxSpeed < lowLevel) " ⛉" else " ⛊"
                            }

                            5 -> {
                                txArrow = if (txSpeed < lowLevel) "↑" else "↑"
                                rxArrow = if (rxSpeed < lowLevel) "↓" else "↓"
                            }

                            6 -> {
                                txArrow = if (txSpeed < lowLevel) "⇧" else "⇧"
                                rxArrow = if (rxSpeed < lowLevel) "⇩" else "⇩"
                            }
                        }

                        // 计算上行网速
                        val tx =
                            if (hideLow && !allHideLow && txSpeed < lowLevel)
                                ""
                            else {
                                if (swapPlaces)
                                    "$txArrow${humanReadableByteCount(it.args[0] as Context, txSpeed)}"
                                else
                                    "${humanReadableByteCount(it.args[0] as Context, txSpeed)}$txArrow"
                            }
                        // 计算下行网速
                        val rx =
                            if (hideLow && !allHideLow && rxSpeed < lowLevel)
                                ""
                            else {
                                if (swapPlaces)
                                    "$rxArrow${humanReadableByteCount(it.args[0] as Context, rxSpeed)}"
                                else
                                    "${humanReadableByteCount(it.args[0] as Context, rxSpeed)}$rxArrow"
                            }
                        // 计算总网速
                        val ax =
                            humanReadableByteCount(it.args[0] as Context, newTxBytesFixed + newRxBytesFixed)
                        // 存储是否隐藏慢速的条件的结果
                        val isLowSpeed = hideLow && (txSpeed + rxSpeed) < lowLevel
                        val isAllLowSpeed = hideLow && allHideLow && txSpeed < lowLevel && rxSpeed < lowLevel

                        when {
                            // 如果显示上下行网速并且不开值和单位双排显示，返回上下行网速的字符串
                            doubleUpDown && !fakeDualRow -> {
                                if (isLowSpeed && !isAllLowSpeed) {
                                    it.result = ""
                                } else if (isAllLowSpeed) {
                                    it.result = ""
                                } else {
                                    it.result = "$tx\n$rx"
                                }
                            }
                            // 如果开启值和单位双排显示，返回总网速的字符串
                            fakeDualRow -> {
                                if (isLowSpeed) {
                                    it.result = ""
                                } else {
                                    it.result = ax
                                }
                            }
                            // 其他情况，对隐藏慢速判定，返回空字符串，其余不返回
                            else -> {
                                if (isLowSpeed) {
                                    it.result = ""
                                }
                            }
                        }
                    }
                }
        }
    }
}
