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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.util.TypedValue
import android.widget.TextView
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackground.isIsland
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewHolder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaNotificationControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.getMediaViewHolderFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull

object MediaViewSize : BaseHook() {

    // ==================== 通知中心配置 ====================

    private val ncModifyTextSize by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_text_size")
    }
    private val ncTitleSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_title_size", 180).toFloat() / 10
    }
    private val ncArtistSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_artist_size", 120).toFloat() / 10
    }
    private val ncTimeSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_time_view_text_size", 130).toFloat() / 10
    }

    // ==================== 灵动岛配置 ====================

    private val diModifyTextSize by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_text_size")
    }
    private val diTitleSize by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_title_size", 180).toFloat() / 10
    }
    private val diArtistSize by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_artist_size", 120).toFloat() / 10
    }
    private val diTimeSize by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_time_view_text_size", 130).toFloat() / 10
    }

    override fun init() {
        if (ncModifyTextSize) {
            initNotificationCenter()
        }
        if (isIsland && diModifyTextSize) {
            initDynamicIsland()
        }
    }

    // ==================== 通知中心 ====================

    private fun initNotificationCenter() {
        miuiMediaNotificationControllerImpl?.let { clz ->
            clz.declaredMethods.firstOrNull { it.name.startsWith("updateLayout") }?.let { method ->
                clz.afterHookMethod(method.name) {
                    val mediaViewHolder = it.thisObject.getObjectFieldOrNull("mediaViewHolder")
                        ?: return@afterHookMethod
                    val titleText = mediaViewHolder.getMediaViewHolderFieldAs<TextView>("titleText", false)
                    val artistText = mediaViewHolder.getMediaViewHolderFieldAs<TextView>("artistText", false)
                    titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncTitleSize)
                    artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncArtistSize)
                }
            }
        }

        mediaViewHolderNew?.afterHookConstructor { param ->
            val holder = param.thisObject
            val titleText = holder.getMediaViewHolderFieldAs<TextView>("titleText", false)
            val artistText = holder.getMediaViewHolderFieldAs<TextView>("artistText", false)
            val elapsedTimeView = holder.getMediaViewHolderFieldAs<TextView>("elapsedTimeView", false)
            val totalTimeView = holder.getMediaViewHolderFieldAs<TextView>("totalTimeView", false)
            titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncTitleSize)
            artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncArtistSize)
            elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncTimeSize)
            totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, ncTimeSize)
        }
    }

    // ==================== 灵动岛 ====================

    private fun initDynamicIsland() {
        miuiIslandMediaViewHolder?.afterHookConstructor { param ->
            val holder = param.thisObject
            val titleText = holder.getMediaViewHolderFieldAs<TextView>("titleText", true)
            val artistText = holder.getMediaViewHolderFieldAs<TextView>("artistText", true)
            val elapsedTimeView = holder.getMediaViewHolderFieldAs<TextView>("elapsedTimeView", true)
            val totalTimeView = holder.getMediaViewHolderFieldAs<TextView>("totalTimeView", true)
            titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, diTitleSize)
            artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, diArtistSize)
            elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, diTimeSize)
            totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, diTimeSize)
        }
    }
}
