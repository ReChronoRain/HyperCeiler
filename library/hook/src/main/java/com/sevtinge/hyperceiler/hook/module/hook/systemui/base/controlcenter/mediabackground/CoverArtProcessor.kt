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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.brightness
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.hardwareBlur
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.MediaControlBgDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.TransitionDrawable
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isDarkMode
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils
import kotlin.random.Random

// https://github.com/HowieHChen/XiaomiHelper/blob/eb1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/bg/CoverArtProcessor.kt
class CoverArtProcessor : BgProcessor {
    private val useAnim = PrefsUtils.mPrefsMap.getBoolean("system_ui_control_center_media_control_control_color_anim")

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
        // 获取 Bitmap
        val artworkBitmapE = createBitmap(artwork.intrinsicWidth, artwork.intrinsicHeight)
        val canvasE = Canvas(artworkBitmapE)
        artwork.setBounds(0, 0, artwork.intrinsicWidth, artwork.intrinsicHeight)
        artwork.draw(canvasE)

        // 缩小图片
        val tmpBitmap = artworkBitmapE.scale(132, 132)
        val tmpBitmapXS = artworkBitmapE.scale(tmpBitmap.width / 2, tmpBitmap.height / 2)

        // 创建混合图
        val bigBitmap = createBitmap(tmpBitmap.width * 2, tmpBitmap.height * 2)
        val canvasE2 = Canvas(bigBitmap)

        // 生成随机图
        val rotImages = mutableListOf<Bitmap>()
        for (i in 1..5) {

            // 中心点随机旋转 90°
            val rotateMatrix = Matrix()
            val pivotX = tmpBitmap.width / 2f
            val pivotY = tmpBitmap.height / 2f
            val rotationAngle = Random.nextInt(4) * 90f
            rotateMatrix.postRotate(rotationAngle, pivotX, pivotY)

            // 随机进行翻转和镜像
            val flipHorizontal = Random.nextBoolean()
            val flipVertical = Random.nextBoolean()
            rotateMatrix.postScale(
                if (flipHorizontal) -1f else 1f,
                if (flipVertical) -1f else 1f,
                pivotX,
                pivotY
            )

            val rotatedImage = if (i <= 4) {
                Bitmap.createBitmap(
                    tmpBitmap,
                    0,
                    0,
                    tmpBitmap.width,
                    tmpBitmap.height,
                    rotateMatrix,
                    true
                )
            } else {
                Bitmap.createBitmap(
                    tmpBitmapXS,
                    0,
                    0,
                    tmpBitmapXS.width,
                    tmpBitmapXS.height,
                    rotateMatrix,
                    true
                )
            }
            rotImages.add(rotatedImage)
        }

        // 将随机图绘制到混合大图上
        canvasE2.drawBitmap(rotImages[0], 0f, 0f, null) // 左上角
        canvasE2.drawBitmap(rotImages[1], tmpBitmap.width.toFloat(), 0f, null) // 右上角
        canvasE2.drawBitmap(
            rotImages[2],
            0f,
            tmpBitmap.height.toFloat(),
            null
        ) // 左下角
        canvasE2.drawBitmap(
            rotImages[3],
            tmpBitmap.width.toFloat(),
            tmpBitmap.height.toFloat(),
            null
        ) // 右下角
        canvasE2.drawBitmap(
            rotImages[4],
            tmpBitmap.width / 4f * 3f,
            tmpBitmap.height / 4f * 3f,
            null
        ) // 中心

        // 颜色处理
        val brightness = bigBitmap.brightness()
        val colorMatrix = brightness.colorMatrix()
        val paintE = Paint()
        paintE.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvasE2.drawBitmap(bigBitmap, 0f, 0f, paintE)
        canvasE2.drawColor(colorConfig.bgStartColor and 0x6FFFFFFF)

        val backgroundColorMode = if (isDarkMode()) 0 else 248
        val backgroundColor = Color.argb(
            20, backgroundColorMode, backgroundColorMode, backgroundColorMode
        )

        // 应用颜色过滤器
        val paintOverlay = Paint()
        paintOverlay.colorFilter =
            PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP)

        // 叠加颜色
        canvasE2.drawBitmap(bigBitmap, 0f, 0f, null)
        canvasE2.drawColor(backgroundColor)

        return bigBitmap.hardwareBlur(40.0f).toDrawable(context.resources)
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
