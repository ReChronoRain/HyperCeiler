/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.getStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * 基于上网卡的 StateFlow 代理。
 * 始终反映当前上网卡对应的原始 Flow 值，用于双排信号模式下
 * slot 0 代理显示当前上网卡的属性（如 showName、isDataConnected）。
 */
class DataSimFlowProxy(private val defaultValue: Any) {
    @Volatile
    var proxy: Any? = null
        private set

    val originalFlows = ConcurrentHashMap<Int, Any>()

    /**
     * 为指定 slot 注册代理收集。
     * slot 0：创建代理 + 收集自身 Flow；slot 1+：仅在自身为上网卡时更新代理。
     */
    fun setupForSlot(
        slotIndex: Int,
        subId: Int,
        originalFlow: Any,
        isSingleSimMode: () -> Boolean,
    ) {
        originalFlows[subId] = originalFlow
        if (slotIndex == 0) {
            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
            val initValue = if (isSingleSimMode() || subId == defaultDataSubId) {
                getStateFlowValue(originalFlow) ?: defaultValue
            } else {
                originalFlows[defaultDataSubId]?.let { getStateFlowValue(it) } ?: defaultValue
            }
            val p = newReadonlyStateFlow(initValue)
            proxy = p
            MiuiStub.javaAdapter.alwaysCollectFlow(originalFlow, Consumer<Any> { value ->
                if (isSingleSimMode() || subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                    setStateFlowValue(p, value)
                }
            })
        } else {
            MiuiStub.javaAdapter.alwaysCollectFlow(originalFlow, Consumer<Any> { value ->
                if (subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                    proxy?.let { p -> setStateFlowValue(p, value) }
                }
            })
        }
    }

    /** 广播触发时同步代理至当前上网卡的值 */
    fun syncFromBroadcast(slot0SubId: Int) {
        val activeDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        val targetFlow = originalFlows[activeDataSubId] ?: originalFlows[slot0SubId]
        if (targetFlow != null) {
            proxy?.let { p ->
                setStateFlowValue(p, getStateFlowValue(targetFlow) ?: defaultValue)
            }
        }
    }
}

/** 移动网络视图遍历与查找工具，封装 a15/a16 的差异 */
object MobileViewHelper {
    private const val TAG = "MobileViewHelper"

    /** 判断当前是否为单卡模式 */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun isSingleSimMode(): Boolean {
        return try {
            val sm = EzXposed.appContext.getSystemService(SubscriptionManager::class.java)
            sm.activeSubscriptionInfoCount <= 1
        } catch (_: Throwable) {
            false
        }
    }

    /** 判断当前是否开启飞行模式 */
    fun isAirplaneModeOn(): Boolean {
        return try {
            Settings.Global.getInt(
                EzXposed.appContext.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (_: Throwable) {
            false
        }
    }

    // ==================== 网络状态检测 ====================
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isWifiConnected(): Boolean {
        val cm = EzXposed.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isMobileDataConnected(): Boolean {
        val cm = EzXposed.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /** 按 subId 遍历关联的 ModernStatusBarMobileView */
    fun forEachMobileView(
        subId: Int,
        viewCache: ConcurrentHashMap<Int, MutableSet<ViewGroup>>,
        callback: (View) -> Unit
    ) {
        viewCache[subId]?.forEach { callback(it) }
    }

    /** 通过 IconController 遍历所有 mobile 视图 */
    fun forEachMobileViewFromController(
        controller: Any,
        callback: (View, Int) -> Unit
    ) {
        when (val iconGroups = controller.getObjectFieldAs<Any>("mIconGroups")) {
            is ArrayList<*> -> {
                iconGroups.filterNotNull().forEach { iconManager ->
                    val group = iconManager.getObjectFieldAs<ViewGroup>("mGroup")
                    forEachMobileChild(group, callback)
                }
            }
            is Map<*, *> -> {
                iconGroups.keys.filterNotNull().forEach { iconManager ->
                    val group = iconManager.getObjectFieldAs<ViewGroup>("mGroup")
                    forEachMobileChild(group, callback)
                }
            }
        }
    }

    private fun forEachMobileChild(group: ViewGroup, callback: (View, Int) -> Unit) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if ("ModernStatusBarMobileView" == child::class.java.simpleName) {
                val subId = child.getIntField("subId")
                callback(child, subId)
            }
        }
    }

    fun collectFlow(view: View, flow: Any, consumer: Consumer<Any>) {
        try {
            val javaAdapterKt = loadClass("com.android.systemui.util.kotlin.JavaAdapterKt")
            EzxHelpUtils.callStaticMethod(
                javaAdapterKt, "collectFlow", view, flow, consumer
            )
        } catch (e: Throwable) {
            XposedLog.e(TAG, "com.android.systemui", "collectFlow error", e)
        }
    }
}
