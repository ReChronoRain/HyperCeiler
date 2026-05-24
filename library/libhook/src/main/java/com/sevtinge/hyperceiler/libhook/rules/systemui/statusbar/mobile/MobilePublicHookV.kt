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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam
import java.util.concurrent.ConcurrentHashMap

class MobilePublicHookV : BaseHook() {

    private val visibilityFlows = ConcurrentHashMap<Int, Any>()

    @Volatile
    private var broadcastRegistered = false

    @Volatile
    private var pairCtor: java.lang.reflect.Constructor<*>? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
    override fun init() {
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
                        visibilityFlows[subId] = isVisible
                        refreshVisibility(subId, isVisible)
                        registerReceiver(EzXposed.appContext)
                    }
                    // 双排信号（signalShowMode == 0 且未隐藏卡）
                    isEnableDouble && !(card1 || card2) -> {
                        cellularIcon.setObjectField("isVisible", isVisible)
                        visibilityFlows[subId] = isVisible
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
                    cellularIcon.setObjectField("smallRoamVisible", newReadonlyStateFlow(false))
                    cellularIcon.setObjectField("mobileRoamVisible", newReadonlyStateFlow(false))
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
        broadcastRegistered = true

        val filter = IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.intent.action.SIM_STATE_CHANGED")
            addAction("android.intent.action.AIRPLANE_MODE")
        }
        context.registerReceiver(object : BroadcastReceiver() {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshAllVisibility()
            }
        }, filter, Context.RECEIVER_EXPORTED)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
                override fun onAvailable(network: Network) = refreshAllVisibility()

                @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
                override fun onLost(network: Network) = refreshAllVisibility()

                @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE])
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refreshAllVisibility()
                }
            }
        )
    }

    private fun updateIconState(param: HookParam, fieldName: String, key: String) {
        val opt = PrefsBridge.getStringAsInt(key, 0)
        if (opt != 0) {
            param.thisObject.setObjectField(fieldName, newReadonlyStateFlow(opt == 1))
        }
    }
}
