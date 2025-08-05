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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media

import android.util.TypedValue
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.mediaViewHolder
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaNotificationControllerImpl
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object MediaViewSize : BaseHook() {

    private val titleSize by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_title_size", 180).toFloat() / 10
    }
    private val artistSize by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_artist_size", 120).toFloat() / 10
    }
    private val timeSize by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_time_view_text_size", 130).toFloat()  / 10
    }
    private val isAndroidB by lazy {
        isMoreAndroidVersion(36)
    }


    override fun init() {
        if (isAndroidB) {
            miuiMediaNotificationControllerImpl!!.methodFinder().filterByName("updateLayout$6")
                .first().createAfterHook {
                    val mediaViewHolder = it.thisObject.getObjectFieldOrNull("mediaViewHolder")
                        ?: return@createAfterHook
                    val titleText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText")
                    val artistText = mediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText")

                    titleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
                    artistText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistSize)
                }
        }

        if (isAndroidB) {
            mediaViewHolderNew!!
        } else {
            mediaViewHolder!!
        }.constructorFinder().first().createAfterHook {
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
