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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.hardwareBlur
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.MediaControlBgDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.RadialMaskedDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.TransitionDrawable
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils
import kotlin.math.max

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/bg/BlurredCoverProcessor.kt
class BlurredCoverProcessor : BgProcessor {
    private val blurRadius = PrefsUtils.mPrefsMap.getInt("system_ui_control_center_media_control_panel_background_blur", 10).coerceIn(1, 20)
    private val useAnim = PrefsUtils.mPrefsMap.getBoolean("system_ui_control_center_media_control_control_color_anim")

    override fun convertToColorConfig(
        artwork: Drawable,
        neutral1: List<Int>,
        neutral2: List<Int>,
        accent1: List<Int>,
        accent2: List<Int>
    ): MediaViewColorConfig {
        return MediaViewColorConfig(
            neutral1[1],
            neutral2[3],
            accent2[9],
            accent1[9]
        )
    }

    override fun processAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig,
        context: Context,
        width: Int,
        height: Int
    ): Drawable {
        val bitmap = RadialMaskedDrawable(artwork, colorConfig.bgStartColor, colorConfig.bgEndColor)
            .toBitmap()
            .hardwareBlur(height.toFloat() / 100 * blurRadius)
        val finalSize = max(bitmap.width, bitmap.height)
        val newBitmap = createBitmap(finalSize, finalSize)
        val canvas = Canvas(newBitmap)
        val deltaW = (bitmap.width - finalSize) / 2f
        val deltaH = (bitmap.height - finalSize) / 2f
        canvas.drawBitmap(bitmap, -deltaW, -deltaH, Paint())
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return newBitmap.toDrawable(context.resources)
    }

    override fun createBackground(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig
    ): MediaControlBgDrawable {
        return TransitionDrawable(artwork, colorConfig, useAnim)
    }
}
