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
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.brightness
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.toSquare
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.LinearGradientDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.MediaControlBgDrawable
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/bg/LinearGradientProcessor.kt
class LinearGradientProcessor : BgProcessor {
    private val allowReverse = PrefsUtils.mPrefsMap.getBoolean("system_ui_control_center_media_control_inverse_color")
    private val useAnim = PrefsUtils.mPrefsMap.getBoolean("system_ui_control_center_media_control_control_color_anim")

    override fun convertToColorConfig(
        artwork: Drawable,
        neutral1: List<Int>,
        neutral2: List<Int>,
        accent1: List<Int>,
        accent2: List<Int>
    ): MediaViewColorConfig {
        // 获取 Bitmap
        val artworkBitmapE = createBitmap(artwork.intrinsicWidth, artwork.intrinsicHeight)
        val canvasE = Canvas(artworkBitmapE)
        artwork.setBounds(0, 0, artwork.intrinsicWidth, artwork.intrinsicHeight)
        artwork.draw(canvasE)

        // 缩小图片
        val tmpBitmap = artworkBitmapE.scale(132, 132)
        val tmpBitmapXS = artworkBitmapE.scale(tmpBitmap.width / 2, tmpBitmap.height / 2)
        val textPrimary: Int
        val backgroundPrimary: Int
        if (allowReverse && tmpBitmapXS.brightness() >= 192) {
            textPrimary = accent1[8]
            backgroundPrimary = accent1[3]
        } else {
            textPrimary = accent1[2]
            backgroundPrimary = accent1[8]
        }
        return MediaViewColorConfig(
            textPrimary,
            textPrimary,
            backgroundPrimary,
            backgroundPrimary
        )
    }

    override fun processAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig,
        context: Context,
        width: Int,
        height: Int
    ): Drawable {
        return artwork.toSquare(context.resources, true, colorConfig.bgStartColor)
    }

    override fun createBackground(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig
    ): MediaControlBgDrawable {
        return LinearGradientDrawable(
            artwork,
            colorConfig,
            useAnim
        )
    }
}
