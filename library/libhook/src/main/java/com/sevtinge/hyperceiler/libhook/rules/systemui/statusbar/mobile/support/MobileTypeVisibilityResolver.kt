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

internal class MobileTypeVisibilityResolver(
    private val showMobileType: Boolean,
    private val mobileNetworkType: Int,
    private val isEnableDouble: Boolean,
    private val isSingleSimMode: () -> Boolean,
) {
    fun shouldUseDualRowDataSimSync(): Boolean {
        return isEnableDouble && !isSingleSimMode()
    }

    fun resolveLargeMobileTypeVisibility(
        mobileTypeSingleVisible: Boolean?,
        isWifiDefaultConnection: Boolean?,
        fallbackVisible: Boolean,
        forceDualMode: Boolean = false,
    ): Boolean {
        val baseVisible = mobileTypeSingleVisible ?: fallbackVisible
        val useDualMode = forceDualMode || shouldUseDualRowDataSimSync()
        if (!showMobileType) return false
        return when (mobileNetworkType) {
            1 -> true
            3 -> false
            2 -> {
                if (useDualMode) {
                    when (isWifiDefaultConnection) {
                        true -> false
                        false -> true
                        null -> baseVisible
                    }
                } else {
                    mobileTypeSingleVisible ?: (isWifiDefaultConnection?.not() ?: true)
                }
            }
            0 -> {
                if (useDualMode) {
                    when (isWifiDefaultConnection) {
                        true -> false
                        false -> true
                        null -> baseVisible
                    }
                } else {
                    mobileTypeSingleVisible ?: (isWifiDefaultConnection?.not() ?: baseVisible)
                }
            }
            4 -> baseVisible
            else -> baseVisible
        }
    }

    fun resolveSmallMobileTypeVisibility(
        mobileTypeVisible: Boolean?,
        wifiConnected: Boolean?,
        dataConnected: Boolean?,
        fallbackVisible: Boolean,
    ): Boolean {
        val baseVisible = mobileTypeVisible ?: fallbackVisible
        if (showMobileType) return false
        return when (mobileNetworkType) {
            2 -> wifiConnected?.not() ?: baseVisible
            4 -> {
                when {
                    wifiConnected == true -> false
                    dataConnected != null -> dataConnected
                    else -> baseVisible
                }
            }
            else -> baseVisible
        }
    }

    fun shouldRefreshSmallMobileTypeDrawable(
        resolvedSmallVisible: Boolean,
        showName: String,
    ): Boolean {
        return !showMobileType &&
            shouldUseDualRowDataSimSync() &&
            (mobileNetworkType == 0 || mobileNetworkType == 2 || mobileNetworkType == 4) &&
            (mobileNetworkType == 0 || resolvedSmallVisible) &&
            showName.isNotEmpty()
    }
}
