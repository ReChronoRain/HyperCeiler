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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2

import android.util.TypedValue
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaViewHolder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaNotificationControllerImpl
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs

object MediaViewSize : BaseHook() {

    private val titleSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_title_size", 180).toFloat() / 10
    }
    private val artistSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_artist_size", 120).toFloat() / 10
    }
    private val timeSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_time_view_text_size", 130).toFloat()  / 10
    }
    private val isAndroidB by lazy {
        isMoreAndroidVersion(36)
    }


    override fun init() {
        if (isAndroidB) {
            miuiMediaNotificationControllerImpl!!
                .findMethod {
                    findOnlyClass()
                    name("updateLayout$6")
                }
                .createAfterHook {
                    val mediaViewHolder = it.thisObject.getObjectFieldOrNull("mediaViewHolder")
                        ?: return@createAfterHook
                    val titleText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
                    val artistText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")

                    titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
                    artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistSize)
                }
        }

        val holderClass = if (isAndroidB) {
            mediaViewHolderNew!!
        } else {
            mediaViewHolder!!
        }
        Constructors.find(holderClass).first().createAfterHook {
            val mediaViewHolder = it.thisObject
            val titleText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
            val artistText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")
            val elapsedTimeView = mediaViewHolder.getObjectFieldOrNullAs<TextView>("elapsedTimeView")
            val totalTimeView = mediaViewHolder.getObjectFieldOrNullAs<TextView>("totalTimeView")

            titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
            artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistSize)
            elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, timeSize)
            totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, timeSize)
        }
    }
}
