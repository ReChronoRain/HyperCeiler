package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Pair
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.net.NetworkInterface
import kotlin.math.pow
import kotlin.math.roundToLong

object NetworkSpeed : BaseHook() {
    private var measureTime: Long = 0
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
                val iface = list.nextElement()
                if (iface.isUp && !iface.isVirtual && !iface.isLoopback && !iface.isPointToPoint && "" != iface.name) {
                    tx += XposedHelpers.callStaticMethod(
                        TrafficStats::class.java,
                        "getTxBytes",
                        iface.name
                    ) as Long
                    rx += XposedHelpers.callStaticMethod(
                        TrafficStats::class.java,
                        "getRxBytes",
                        iface.name
                    ) as Long
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log(t)
            tx = TrafficStats.getTotalTxBytes()
            rx = TrafficStats.getTotalRxBytes()
        }
        return Pair(tx, rx)
    }

//  网速计算与隐藏相关
    @SuppressLint("DefaultLocale")
    private fun humanReadableByteCount(ctx: Context, bytes: Long): String {
        return try {
            val modRes = Helpers.getModuleRes(ctx)
            val hideSecUnit = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit")
            var unitSuffix = modRes.getString(R.string.system_ui_statusbar_network_speed_Bs)
            if (hideSecUnit) {
                unitSuffix = ""
            }
            var f = bytes / 1024.0f
            var expIndex = 0
            if (f > 999.0f) {
                expIndex = 1
                f /= 1024.0f
            }
            val pre =
                modRes.getString(R.string.system_ui_statusbar_network_speed_speedunits)[expIndex]
            (if (f < 100.0f) String.format("%.1f", f) else String.format(
                "%.0f",
                f
            )) + String.format(
                "%s$unitSuffix", pre
            )
        } catch (t: Throwable) {
            Helpers.log(t)
            ""
        }
    }

    override fun init() {
//      双排网速相关
        val nscCls = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.policy.NetworkSpeedController",
            lpparam.classLoader
        )

        if (nscCls == null) {
            Helpers.log("DetailedNetSpeedHook", "No NetworkSpeed view or controller")
        } else {
            Helpers.findAndHookMethod(nscCls, "getTotalByte", object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val bytes = getTrafficBytes()
                    txBytesTotal = bytes.first
                    rxBytesTotal = bytes.second
                    measureTime = System.nanoTime()
                }
            })
            Helpers.findAndHookMethod(nscCls, "updateNetworkSpeed", object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    var isConnected = false
                    val mContext =
                        XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                    val mConnectivityManager =
                        mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val nw = mConnectivityManager.activeNetwork
                    if (nw != null) {
                        val capabilities = mConnectivityManager.getNetworkCapabilities(nw)
                        if (capabilities != null && (!(!capabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_WIFI
                            ) && !capabilities.hasTransport(
                                NetworkCapabilities.TRANSPORT_CELLULAR
                            )))
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
                        var newTxBytesFixed = newTxBytes - txBytesTotal
                        var newRxBytesFixed = newRxBytes - rxBytesTotal
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
            })
            Helpers.findAndHookMethod(nscCls, "updateText",
                String::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val mContext =
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
//                      隐藏慢速
                        val hideLow = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide")
//                      慢速水平
                        val lowLevel = mPrefsMap.getInt(
                            "system_ui_statusbar_network_speed_hide_slow",
                            1
                        ) * 1024
//                      网速图标
                        val icons =
                            mPrefsMap.getString("system_ui_statusbar_network_speed_icon", "2").toInt()
                        var txarrow = ""
                        var rxarrow = ""
                        when (icons) {
                            2 -> {
                                txarrow = if (txSpeed < lowLevel) "△" else "▲"
                                rxarrow = if (rxSpeed < lowLevel) "▽" else "▼"
                            }
                            3 -> {
                                txarrow = if (txSpeed < lowLevel) " ▵" else " ▴"
                                rxarrow = if (rxSpeed < lowLevel) " ▿" else " ▾"
                            }
                            4 -> {
                                txarrow = if (txSpeed < lowLevel) " ☖" else " ☗"
                                rxarrow = if (rxSpeed < lowLevel) " ⛉" else " ⛊"
                            }
                            5 -> {
                                txarrow = if (txSpeed < lowLevel) "↑" else "↑"
                                rxarrow = if (rxSpeed < lowLevel) "↓" else "↓"
                            }
                            6 -> {
                                txarrow = if (txSpeed < lowLevel) "⇧" else "⇧"
                                rxarrow = if (rxSpeed < lowLevel) "⇩" else "⇩"
                            }
                        }
                        val tx = if (hideLow && txSpeed < lowLevel) "" else humanReadableByteCount(mContext, txSpeed) + txarrow
                        val rx = if (hideLow && rxSpeed < lowLevel) "" else humanReadableByteCount(mContext, rxSpeed) + rxarrow
                        param.args[0] = """
                    $tx
                    $rx
                    """.trimIndent()
                    }
                }
            )
        }
    }
}