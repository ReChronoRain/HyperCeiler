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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.card1
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.card2
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.hideRoaming
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.isEnableDouble
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.signalShowMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper.isMobileDataConnected
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper.isWifiConnected
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.hookAllConstructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import java.util.concurrent.ConcurrentHashMap

class MobilePublicHookV : BaseHook() {
    private companion object {
        const val STATE_CONTEXT = "MobilePublicHookV.context"
        const val STATE_SUB_IDS = "MobilePublicHookV.subIds"
        const val STATE_FLOW_PREFIX = "MobilePublicHookV.flow."
    }

    private val visibilityFlows = ConcurrentHashMap<Int, Any>()

    @Volatile
    private var broadcastRegistered = false

    @Volatile
    private var pairCtor: java.lang.reflect.Constructor<*>? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
    override fun init() {
        restoreVisibilityFlowsAfterHotReload()

        miuiCellularIconVM.hookAllConstructors {
            after { param ->
                val cellularIcon = param.thisObject
                val mobileIconInteractor = param.args[2] ?: return@after
                val subId = mobileIconInteractor.getObjectFieldAs<Int>("subId")
                val isVisible = createVisibilityFlow()

                when {
                    // 信号显示逻辑
                    signalShowMode >= 1 -> {
                        cellularIcon.setObjectField("isVisible", isVisible)
                        trackVisibilityFlow(subId, isVisible)
                        refreshVisibility(subId, isVisible)
                        registerReceiver(EzXposed.appContext)
                    }
                    // 双排信号（signalShowMode == 0 且未隐藏卡）
                    isEnableDouble && !(card1 || card2) -> {
                        cellularIcon.setObjectField("isVisible", isVisible)
                        trackVisibilityFlow(subId, isVisible)
                        val slotIndex = SubscriptionManager.getSlotIndex(subId)
                        val shouldShow = !MobileViewHelper.isAirplaneModeOn() &&
                            (MobileViewHelper.isSingleSimMode() || slotIndex == 0)
                        updateVisibility(isVisible, shouldShow)
                        registerReceiver(EzXposed.appContext)
                    }
                    // 隐藏指定卡
                    else -> {
                        val slotIndex = SubscriptionManager.getSlotIndex(subId)
                        if ((card1 && slotIndex == 0) || (card2 && slotIndex == 1)) {
                            cellularIcon.setObjectField("isVisible", isVisible)
                        }
                    }
                }

                if (hideIndicator) {
                    cellularIcon.setObjectField("inOutVisible", newReadonlyStateFlow(false))
                }
                if (hideRoaming) {
                    // 新版 MiuiCellularIconVM 中 *RoamVisible 字段类型为
                    // FlowKt__ZipKt$combine$$inlined$combineUnsafe$FlowKt__ZipKt$1
                    // （combine 产生的匿名 Flow），直接 setObjectField 会抛
                    // IllegalArgumentException。先尝试旧版直接替换 StateFlow，
                    // 失败则改为劫持其内部 $transform$inlined$1 lambda 让合并结果恒为 false。
                    forceRoamHidden(cellularIcon, "smallRoamVisible")
                    forceRoamHidden(cellularIcon, "mobileRoamVisible")
                }
                if (!isMoreSmallVersion(200, 2f)) {
                    updateIconState(param, "smallHdVisible", "system_ui_status_bar_icon_small_hd")
                    updateIconState(param, "volteVisibleCn", "system_ui_status_bar_icon_big_hd")
                    updateIconState(param, "volteVisibleGlobal", "system_ui_status_bar_icon_big_hd")
                }
            }
        }
    }

    // ==================== Flow 工具 ====================
    private fun trackVisibilityFlow(subId: Int, flow: Any) {
        visibilityFlows[subId] = flow
        // ReadonlyStateFlow 由 SystemUI 的 kotlinx.coroutines classloader 创建；只有它不是
        // 模块对象时才跨 generation 保存。这样既能让现有 ViewModel 继续接收更新，也不会把
        // 模块 classloader / lambda 带进 SavedInstanceState。
        if (flow.javaClass.classLoader === javaClass.classLoader) {
            return
        }
        BaseHook.putHotReloadRuntimeState("$STATE_FLOW_PREFIX$subId", flow)
        BaseHook.putHotReloadRuntimeState(
            STATE_SUB_IDS,
            visibilityFlows.keys.sorted().joinToString(",")
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
    private fun restoreVisibilityFlowsAfterHotReload() {
        if (signalShowMode < 1 && !(isEnableDouble && !(card1 || card2))) {
            return
        }
        val savedIds = BaseHook.getHotReloadRuntimeState(STATE_SUB_IDS, String::class.java)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            .orEmpty()
        savedIds.forEach { subId ->
            val flow = BaseHook.getHotReloadRuntimeState("$STATE_FLOW_PREFIX$subId", Any::class.java)
                ?: return@forEach
            if (flow.javaClass.classLoader !== javaClass.classLoader) {
                visibilityFlows[subId] = flow
            }
        }
        if (visibilityFlows.isEmpty()) {
            return
        }
        BaseHook.getHotReloadRuntimeState(STATE_CONTEXT, Context::class.java)?.let(::registerReceiver)
        refreshAllVisibility()
    }

    private fun needPairFlow(): Boolean {
        return isMoreAndroidVersion(36) || isMoreHyperOSVersion(3f)
    }

    private fun newPair(value: Boolean): Any {
        val ctor = pairCtor ?: loadClass("kotlin.Pair")
            .getConstructor(Object::class.java, Object::class.java)
            .also { pairCtor = it }
        return ctor.newInstance(value, value)
    }

    private fun createVisibilityFlow(): Any {
        return if (needPairFlow()) {
            newReadonlyStateFlow(newPair(false))
        } else {
            newReadonlyStateFlow(false)
        }
    }

    private fun updateVisibility(isVisible: Any, shouldShow: Boolean) {
        if (needPairFlow()) {
            setStateFlowValue(isVisible, newPair(shouldShow))
        } else {
            setStateFlowValue(isVisible, shouldShow)
        }
    }


    /**
     * 隐藏漫游图标。
     *
     * 旧版 MiuiCellularIconVM 中 *RoamVisible 是 StateFlow，可以直接替换；
     * 新版被改成了 combine(...) 产生的匿名 Flow（FlowKt__ZipKt$combine$$inlined$combineUnsafe$…），
     * 反射赋值会报 IllegalArgumentException。
     *
     * 该匿名类内部字段：
     *  - \$flows\$inlined        : Flow[]
     *  - \$transform\$inlined\$1  : Function (Function4/5/6)
     *  - \$r8\$classId           : Int   (R8 合并分支标识，不同值对应不同元数 lambda)
     *
     * 思路：保留字段对象本身，只把 transform lambda 换成一个同接口且永远返回 false
     * 的动态代理。这样 combine 输出恒为 false，运行时几乎零额外开销，且不会再抛异常。
     */
    private fun forceRoamHidden(cellularIcon: Any, fieldName: String) {
        // 先走旧版快路径：直接替换整个 StateFlow。
        if (runCatching {
                cellularIcon.setObjectField(fieldName, newReadonlyStateFlow(false))
            }.isSuccess) return

        // 新版快路径：劫持 combine 内部的 transform lambda。
        runCatching {
            val flowObj = cellularIcon.getObjectFieldAs<Any>(fieldName)
            val fakeTransform = createAlwaysFalseTransform(flowObj) ?: return
            flowObj.setObjectField($$"$transform$inlined$1", fakeTransform)
        }
    }

    /**
     * 根据 combine 匿名 Flow 对象创建一个同元数、恒返 false 的 Function 代理。
     * 通过读取 \$transform\$inlined\$1 原始实现的所有接口来推断 Function 元数，
     * 避免硬编码 \$r8\$classId 与 Function4/5/6 的映射关系（不同版本映射可能不同）。
     */
    private fun createAlwaysFalseTransform(flowObj: Any): Any? {
        val originalTransform = runCatching {
            flowObj.getObjectFieldAs<Any>($$"$transform$inlined$1")
        }.getOrNull() ?: return null

        val interfaces = originalTransform.javaClass.interfaces
            .filter { it.name.startsWith("kotlin.jvm.functions.Function") }
            .toTypedArray()
        if (interfaces.isEmpty()) return null

        val cl = originalTransform.javaClass.classLoader ?: javaClass.classLoader
        return java.lang.reflect.Proxy.newProxyInstance(cl, interfaces) { _, method, _ ->
            when (method.name) {
                "invoke" -> false
                "toString" -> "AlwaysFalseTransform"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        }
    }

    // ==================== 统一可见性刷新 ====================
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
    private fun refreshVisibility(subId: Int, isVisible: Any) {
        val isAirplane = MobileViewHelper.isAirplaneModeOn()
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        val slotIndex = SubscriptionManager.getSlotIndex(subId)

        val shouldShow = when (signalShowMode) {
            1 -> !isAirplane && !isWifiConnected()
            2 -> !isAirplane && subId == defaultDataSubId && isMobileDataConnected()
            3 -> !isAirplane && subId == defaultDataSubId
            else -> !isAirplane && (MobileViewHelper.isSingleSimMode() || slotIndex == 0)
        }
        updateVisibility(isVisible, shouldShow)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
    private fun refreshAllVisibility() {
        val isAirplane = MobileViewHelper.isAirplaneModeOn()
        val isSingleSim = MobileViewHelper.isSingleSimMode()
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()

        visibilityFlows.forEach { (subId, isVisible) ->
            val shouldShow = if (signalShowMode >= 1) {
                when (signalShowMode) {
                    1 -> !isAirplane && !isWifiConnected()
                    2 -> !isAirplane && subId == defaultDataSubId && isMobileDataConnected()
                    3 -> !isAirplane && subId == defaultDataSubId
                    else -> !isAirplane && (isSingleSim || SubscriptionManager.getSlotIndex(subId) == 0)
                }
            } else {
                !isAirplane && (isSingleSim || SubscriptionManager.getSlotIndex(subId) == 0)
            }
            updateVisibility(isVisible, shouldShow)
        }
    }

    // ==================== 广播监听 ====================
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @Synchronized
    private fun registerReceiver(context: Context) {
        if (broadcastRegistered) return

        val filter = IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.intent.action.SIM_STATE_CHANGED")
            addAction("android.intent.action.AIRPLANE_MODE")
        }
        val receiver = object : BroadcastReceiver() {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshAllVisibility()
            }
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        BaseHook.registerReceiverHotReloadCleanup(context, receiver)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
            override fun onAvailable(network: Network) = refreshAllVisibility()

            @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
            override fun onLost(network: Network) = refreshAllVisibility()

            @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refreshAllVisibility()
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        BaseHook.registerNetworkCallbackHotReloadCleanup(cm, callback)
        broadcastRegistered = true
        BaseHook.putHotReloadRuntimeState(STATE_CONTEXT, context)
    }

    private fun updateIconState(param: HookParam, fieldName: String, key: String) {
        val opt = PrefsBridge.getStringAsInt(key, 0)
        if (opt != 0) {
            param.thisObject.setObjectField(fieldName, newReadonlyStateFlow(opt == 1))
        }
    }
}
