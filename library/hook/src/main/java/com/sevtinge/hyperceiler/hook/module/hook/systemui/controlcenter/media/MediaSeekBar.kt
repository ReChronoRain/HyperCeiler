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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.SeekBar
import androidx.core.graphics.toColorInt
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.mediaViewHolder
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.seekBarObserver
import com.sevtinge.hyperceiler.hook.utils.api.dp
import com.sevtinge.hyperceiler.hook.utils.devicesdk.colorFilter
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.helper.ObjectHelper.`-Static`.objectHelper
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object MediaSeekBar : BaseHook() {
    private val progress by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2
    }
    private val progressThickness by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_thickness", 80)
    }
    private val cornerRadiusBar by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_progress_corner_radius", 36)
    }
    private val removeBackground by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_remove_media_control_panel_background")
    }

    override fun init() {
        mediaViewHolder?.constructors?.first()?.createAfterHook {
            val seekBar = it.thisObject.objectHelper()
                    .getObjectFieldOrNullAs<SeekBar>("seekBar")

            val backgroundDrawable = GradientDrawable().apply {
                color = ColorStateList(arrayOf(intArrayOf()), intArrayOf("#20ffffff".toColorInt()))
                cornerRadius = cornerRadiusBar.dp.toFloat()
            }

            val onProgressDrawable = GradientDrawable().apply {
                color = ColorStateList(arrayOf(intArrayOf()), intArrayOf("#ffffffff".toColorInt()))
                cornerRadius = cornerRadiusBar.dp.toFloat()
            }

            val layerDrawable = LayerDrawable(
                arrayOf(backgroundDrawable, ClipDrawable(onProgressDrawable, Gravity.START, ClipDrawable.HORIZONTAL))
            ).apply {
                if (progress) {
                    setLayerHeight(0, progressThickness.dp)
                    setLayerHeight(1, progressThickness.dp)
                } else {
                    setLayerHeight(0, 9.dp)
                    setLayerHeight(1, 9.dp)
                }
            }

            seekBar?.progressDrawable = layerDrawable
        }

        seekBarObserver?.constructors?.first()?.createAfterHook {
            if (progress) {
                it.thisObject.objectHelper()
                    .setObject("seekBarEnabledMaxHeight", progressThickness.dp)
            } else {
                it.thisObject.objectHelper()
                    .setObject("seekBarEnabledMaxHeight", 9.dp)
            }
        }

        if (!removeBackground) {
            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()?.createAfterHook {
                val mMediaViewHolder = it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder") ?: return@createAfterHook

                val seekBar = mMediaViewHolder.objectHelper().getObjectFieldOrNullAs<SeekBar>("seekBar")
                seekBar?.thumb?.colorFilter = colorFilter(Color.TRANSPARENT)
            }
        }
    }
}
