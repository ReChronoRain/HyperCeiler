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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isDarkMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.brightness
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.hardwareBlur
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.MediaControlBgDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.TransitionDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import kotlin.random.Random

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/bg/CoverArtProcessor.kt
class CoverArtProcessor : BgProcessor {
    private val useAnim = PrefsBridge.getBoolean("system_ui_control_center_media_control_control_color_anim")

    override fun convertToColorConfig(
        artwork: Drawable,
        neutral1: List<Int>,
        neutral2: List<Int>,
        accent1: List<Int>,
        accent2: List<Int>
    ): MediaViewColorConfig {
        return MediaViewColorConfig(
            accent1[2],
            accent1[2],
            accent1[8],
            accent1[8]
        )
    }

    override fun processAlbumCover(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig,
        context: Context,
        width: Int,
        height: Int
    ): Drawable {
        // 1. Drawable → Bitmap
        val artworkBitmap = createBitmap(artwork.intrinsicWidth, artwork.intrinsicHeight)
        Canvas(artworkBitmap).also {
            artwork.setBounds(0, 0, artwork.intrinsicWidth, artwork.intrinsicHeight)
            artwork.draw(it)
        }

        // 2. 缩小图片
        val tmpBitmap = artworkBitmap.scale(132, 132)
        val tmpBitmapXS = artworkBitmap.scale(tmpBitmap.width / 2, tmpBitmap.height / 2)
        artworkBitmap.recycle() // 原始大图不再需要

        // 3. 生成马赛克拼贴大图
        val bigBitmap = createMosaicBitmap(tmpBitmap, tmpBitmapXS)
        tmpBitmap.recycle()
        tmpBitmapXS.recycle()

        // 4. 颜色校正
        applyColorCorrection(bigBitmap, colorConfig)

        // 5. 模糊出最终结果
        val result = bigBitmap.hardwareBlur(40.0f).toDrawable(context.resources)
        bigBitmap.recycle()
        return result
    }

    /**
     * 将缩放后的封面通过随机旋转/翻转拼贴成 2x2 + 中心的马赛克大图
     */
    private fun createMosaicBitmap(tile: Bitmap, tileXS: Bitmap): Bitmap {
        val bigBitmap = createBitmap(tile.width * 2, tile.height * 2)
        val canvas = Canvas(bigBitmap)
        val pivotX = tile.width / 2f
        val pivotY = tile.height / 2f
        val reusableMatrix = Matrix()

        // 四角用大 tile，中心用小 tile
        val positions = arrayOf(
            0f to 0f,                                       // 左上
            tile.width.toFloat() to 0f,                     // 右上
            0f to tile.height.toFloat(),                    // 左下
            tile.width.toFloat() to tile.height.toFloat(),  // 右下
            tile.width / 4f * 3f to tile.height / 4f * 3f  // 中心
        )

        for (i in positions.indices) {
            val src = if (i < 4) tile else tileXS
            reusableMatrix.reset()
            reusableMatrix.postRotate(Random.nextInt(4) * 90f, pivotX, pivotY)
            reusableMatrix.postScale(
                if (Random.nextBoolean()) -1f else 1f,
                if (Random.nextBoolean()) -1f else 1f,
                pivotX, pivotY
            )
            val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, reusableMatrix, true)
            canvas.drawBitmap(rotated, positions[i].first, positions[i].second, null)
            if (rotated !== src) rotated.recycle() // 回收旋转产生的中间 Bitmap
        }

        return bigBitmap
    }

    /**
     * 根据亮度对大图进行颜色矩阵校正，并叠加半透明背景色
     */
    private fun applyColorCorrection(bitmap: Bitmap, colorConfig: MediaViewColorConfig) {
        val canvas = Canvas(bitmap)
        val brightness = bitmap.brightness()
        val colorMatrix = brightness.colorMatrix()

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.drawColor(colorConfig.bgStartColor and 0x6FFFFFFF)

        val backgroundColorMode = if (isDarkMode()) 0 else 248
        val backgroundColor = Color.argb(20, backgroundColorMode, backgroundColorMode, backgroundColorMode)

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawColor(backgroundColor)
    }

    override fun createBackground(
        artwork: Drawable,
        colorConfig: MediaViewColorConfig
    ): MediaControlBgDrawable {
        return TransitionDrawable(
            artwork,
            colorConfig,
            useAnim
        )
    }

    private fun Float.colorMatrix(): ColorMatrix {
        val colorMatrix = ColorMatrix()
        val adjustment = when (this) {
            in 0.0f.rangeUntil(50.0f) -> 40.0f
            in 50.0f.rangeUntil(100.0f) -> 20.0f
            in 100.0f.rangeUntil(200.0f) -> -20.0f
            in 200.0f..255.0f -> -40.0f
            else -> 0f
        }
        colorMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, adjustment, // red
                0f, 1f, 0f, 0f, adjustment, // green
                0f, 0f, 1f, 0f, adjustment, // blue
                0f, 0f, 0f, 1f, 0f          // alpha
            )
        )
        return colorMatrix
    }
}


