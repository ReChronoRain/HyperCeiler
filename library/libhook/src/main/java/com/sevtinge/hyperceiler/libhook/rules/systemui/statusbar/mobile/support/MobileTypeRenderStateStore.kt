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

package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support

import android.telephony.SubscriptionManager
import java.util.concurrent.ConcurrentHashMap

internal data class MobileTypeRenderState(
    val showName: String,
    val inOutVisible: Boolean?,
    val mobileTypeSingleVisible: Boolean?,
    val mobileTypeVisible: Boolean?,
    val wifiConnected: Boolean?,
    val dataConnected: Boolean?,
)

internal class MobileTypeRenderStateStore {
    private val subSlotIndices = ConcurrentHashMap<Int, Int>()
    private val showNameStates = ConcurrentHashMap<Int, String>()
    private val inOutVisibleStates = ConcurrentHashMap<Int, Boolean>()
    private val dataConnectedStates = ConcurrentHashMap<Int, Boolean>()
    private val wifiAvailableStates = ConcurrentHashMap<Int, Boolean>()
    private val mobileTypeSingleVisibleStates = ConcurrentHashMap<Int, Boolean>()
    private val mobileTypeVisibleStates = ConcurrentHashMap<Int, Boolean>()

    @Volatile
    private var lastResolvedDataSubId: Int = -1

    fun onCollectorAttached(subId: Int, slotIndex: Int) {
        subSlotIndices[subId] = slotIndex
        inheritStateForReplacementSubId(subId, slotIndex)
    }

    fun updateShowName(subId: Int, showName: String) {
        showNameStates[subId] = showName
    }

    fun updateInOutVisible(subId: Int, value: Boolean) {
        inOutVisibleStates[subId] = value
    }

    fun updateDataConnected(subId: Int, value: Boolean) {
        dataConnectedStates[subId] = value
    }

    fun updateWifiAvailable(subId: Int, value: Boolean) {
        wifiAvailableStates[subId] = value
    }

    fun updateMobileTypeSingleVisible(subId: Int, value: Boolean) {
        mobileTypeSingleVisibleStates[subId] = value
    }

    fun updateMobileTypeVisible(subId: Int, value: Boolean) {
        mobileTypeVisibleStates[subId] = value
    }

    fun updateDataConnectedBySlots(states: BooleanArray) {
        subSlotIndices.forEach { (subId, slotIndex) ->
            dataConnectedStates[subId] = slotIndex in states.indices && states[slotIndex]
        }
    }

    fun findSlot0SubId(): Int {
        return subSlotIndices.entries.firstOrNull { (_, slotIndex) ->
            slotIndex == 0
        }?.key ?: -1
    }

    fun snapshot(targetSubId: Int): MobileTypeRenderState {
        return MobileTypeRenderState(
            showName = showNameStates[targetSubId] ?: "",
            inOutVisible = inOutVisibleStates[targetSubId],
            mobileTypeSingleVisible = mobileTypeSingleVisibleStates[targetSubId],
            mobileTypeVisible = mobileTypeVisibleStates[targetSubId],
            wifiConnected = wifiAvailableStates[targetSubId],
            dataConnected = dataConnectedStates[targetSubId]
        )
    }

    fun resolveRenderSubId(
        viewSubId: Int,
        isEnableDouble: Boolean,
        isSingleSimMode: Boolean,
    ): Int {
        if (!isEnableDouble || isSingleSimMode) {
            return viewSubId
        }

        val slotIndex = SubscriptionManager.getSlotIndex(viewSubId)
        if (slotIndex != 0) {
            return viewSubId
        }

        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (isRenderStateReady(defaultDataSubId)) {
            lastResolvedDataSubId = defaultDataSubId
            return defaultDataSubId
        }

        if (isRenderStateReady(lastResolvedDataSubId)) {
            return lastResolvedDataSubId
        }

        return if (isRenderStateReady(viewSubId)) {
            viewSubId
        } else if (hasTrackedState(defaultDataSubId)) {
            defaultDataSubId
        } else {
            viewSubId
        }
    }

    private fun inheritStateForReplacementSubId(subId: Int, slotIndex: Int) {
        if (slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) return
        if (hasTrackedState(subId)) return

        val sourceSubId = subSlotIndices.entries.firstOrNull { (oldSubId, oldSlotIndex) ->
            oldSubId != subId && oldSlotIndex == slotIndex && hasTrackedState(oldSubId)
        }?.key ?: return

        showNameStates[sourceSubId]?.takeIf { it.isNotEmpty() }?.let { showNameStates[subId] = it }
        inOutVisibleStates[sourceSubId]?.let { inOutVisibleStates[subId] = it }
        dataConnectedStates[sourceSubId]?.let { dataConnectedStates[subId] = it }
        wifiAvailableStates[sourceSubId]?.let { wifiAvailableStates[subId] = it }
        mobileTypeSingleVisibleStates[sourceSubId]?.let { mobileTypeSingleVisibleStates[subId] = it }
        mobileTypeVisibleStates[sourceSubId]?.let { mobileTypeVisibleStates[subId] = it }
    }

    private fun hasTrackedState(subId: Int): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return false
        return showNameStates.containsKey(subId) ||
            inOutVisibleStates.containsKey(subId) ||
            dataConnectedStates.containsKey(subId) ||
            wifiAvailableStates.containsKey(subId) ||
            mobileTypeSingleVisibleStates.containsKey(subId) ||
            mobileTypeVisibleStates.containsKey(subId)
    }

    private fun isRenderStateReady(subId: Int?): Boolean {
        if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId)) return false
        if (!hasTrackedState(subId)) return false
        return true
    }
}
