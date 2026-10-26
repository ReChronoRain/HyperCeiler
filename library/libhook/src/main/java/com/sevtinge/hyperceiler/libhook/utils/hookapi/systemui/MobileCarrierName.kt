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

import android.content.Context
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

/**
 * 控制中心运营商名称的取值入口（与 [MobilePrefs] 一样集中存放相关开关）。
 *
 * 取值优先级：用户手填的名称（按物理卡槽区分）> 当前注册的真实网络名称。
 * 两者都拿不到时返回 null，由调用方保留 ROM 的原始文字。
 */
object MobileCarrierName {
    private const val FIRST_SLOT_PREF_KEY = "system_ui_control_center_real_network_name_slot1"
    private const val SECOND_SLOT_PREF_KEY = "system_ui_control_center_real_network_name_slot2"

    /**
     * @param slot 物理卡槽（0 或 1）
     * @return 需要显示的名称；返回 null 表示保持原文
     */
    fun resolve(slot: Int): String? {
        readCustomName(slot)?.let { return it }
        // 需要宿主 Context 才能取到电话服务；拿不到就退回原文。
        val context = EzXposed.appContextOrNull ?: return null
        return CarrierNetworkName.resolve(context, slot)
    }

    private fun readCustomName(slot: Int): String? {
        val key = when (slot) {
            0 -> FIRST_SLOT_PREF_KEY
            1 -> SECOND_SLOT_PREF_KEY
            // 非法的卡槽值一律不替换。
            else -> return null
        }
        return PrefsBridge.getString(key, "").trim().takeIf { it.isNotEmpty() }
    }
}
