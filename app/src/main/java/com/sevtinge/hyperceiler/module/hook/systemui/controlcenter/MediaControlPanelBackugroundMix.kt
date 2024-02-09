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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.HardwareRenderer
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isDarkMode
import kotlin.math.sqrt
import kotlin.random.Random

private var artwork: Icon? = null

//from https://github.com/YuKongA/MediaControl-BlurBg/blob/752de17a31d940683648cee7b957d4ff48d381a3/app/src/main/kotlin/top/yukonga/mediaControlBlur/MainHook.kt
class MediaControlPanelBackupMix : BaseHook() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun init() {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)

        // 部分代码来自 Hyper Helper (https://github.com/HowieHChen/XiaomiHelper/blob/master/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/CustomMusicControl.kt)
        try {
            val miuiMediaControlPanel =
                loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
            val playerTwoCircleView =
                loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.PlayerTwoCircleView")

            //  获取 Icon
            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()?.createHook {
                before {
                    artwork = it.args[0].objectHelper().getObjectOrNullAs<Icon>("artwork")
                        ?: return@before
                }
            }

            // 重写 onDraw
            playerTwoCircleView?.methodFinder()?.filterByName("onDraw")?.first()?.createHook {
                before {
                    (it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint1"))?.alpha = 0
                    (it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint2"))?.alpha = 0
                    it.thisObject.objectHelper().setObject("mRadius", 0.0f)
                }
            }

            // 重写 setBackground
            playerTwoCircleView?.methodFinder()?.filterByName("setBackground")?.first()
                ?.createHook {
                    replace {
                        if (artwork == null) return@replace it
                        val imageView = it.thisObject as ImageView
                        val backgroundColors = it.args[0] as IntArray

                        // 获取 Bitmap
                        var artworkLayer =
                            artwork?.loadDrawable(imageView.context) ?: return@replace it
                        val artworkBitmap = Bitmap.createBitmap(
                            artworkLayer.intrinsicWidth,
                            artworkLayer.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(artworkBitmap)
                        artworkLayer.setBounds(
                            0,
                            0,
                            artworkLayer.intrinsicWidth,
                            artworkLayer.intrinsicHeight
                        )
                        artworkLayer.draw(canvas)

                        // 缩小图片
                        val tmpBitmap = Bitmap.createScaledBitmap(artworkBitmap, 132, 132, true)
                        val tmpBitmapXS = Bitmap.createScaledBitmap(
                            artworkBitmap,
                            tmpBitmap.width / 2,
                            tmpBitmap.height / 2,
                            true
                        )

                        // 创建混合图
                        val bigBitmap = Bitmap.createBitmap(
                            tmpBitmap.width * 2,
                            tmpBitmap.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas2 = Canvas(bigBitmap)

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
                        canvas2.drawBitmap(rotImages[0], 0f, 0f, null) // 左上角
                        canvas2.drawBitmap(rotImages[1], tmpBitmap.width.toFloat(), 0f, null) // 右上角
                        canvas2.drawBitmap(
                            rotImages[2],
                            0f,
                            tmpBitmap.height.toFloat(),
                            null
                        ) // 左下角
                        canvas2.drawBitmap(
                            rotImages[3],
                            tmpBitmap.width.toFloat(),
                            tmpBitmap.height.toFloat(),
                            null
                        ) // 右下角
                        canvas2.drawBitmap(
                            rotImages[4],
                            tmpBitmap.width / 4f * 3f,
                            tmpBitmap.height / 4f * 3f,
                            null
                        ) // 中心

                        // 颜色处理
                        val brightness = bigBitmap.brightness()
                        val colorMatrix = brightness.colorMatrix()
                        val paint = Paint()
                        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                        canvas2.drawBitmap(bigBitmap, 0f, 0f, paint)
                        canvas2.drawColor(backgroundColors[0] and 0x6FFFFFFF)

                        val backgroundColorMode = if (isDarkMode()) 0 else 248
                        val backgroundColor = Color.argb(mPrefsMap.getInt("system_ui_control_center_media_control_panel_background_mix_overlay", 20), backgroundColorMode, backgroundColorMode, backgroundColorMode)

                        // 应用颜色过滤器
                        val paintOverlay = Paint()
                        paintOverlay.colorFilter = PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP)

                        // 叠加颜色
                        canvas2.drawBitmap(bigBitmap, 0f, 0f, null)
                        canvas2.drawColor(backgroundColor)

                        // 模糊处理
                        artworkLayer = BitmapDrawable(
                            imageView.resources, bigBitmap.blur(
                                mPrefsMap.getInt(
                                    "system_ui_control_center_media_control_panel_background_mix_blur_radius",
                                    40
                                )
                                    .toFloat()
                            )
                        )

                        // 绘制到 ImageView 上
                        imageView.setImageDrawable(artworkLayer)
                    }

                }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Bitmap.blur(radius: Float): Bitmap {

    // 该部分来自 Google (https://developer.android.google.cn/guide/topics/renderscript/migrate)
    val imageReader =
        ImageReader.newInstance(
            this.width,
            this.height,
            PixelFormat.RGBA_8888,
            1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
    val renderNode = RenderNode("BlurEffect")
    val hardwareRenderer = HardwareRenderer()

    hardwareRenderer.setSurface(imageReader.surface)
    hardwareRenderer.setContentRoot(renderNode)
    renderNode.setPosition(0, 0, imageReader.width, imageReader.height)
    val blurRenderEffect = RenderEffect.createBlurEffect(
        radius, radius, Shader.TileMode.MIRROR
    )
    renderNode.setRenderEffect(blurRenderEffect)

    val renderCanvas = renderNode.beginRecording()
    renderCanvas.drawBitmap(this, 0f, 0f, null)
    renderNode.endRecording()
    hardwareRenderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

    val image = imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
    val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
        ?: throw RuntimeException("Create Bitmap Failed")

    hardwareBuffer.close()
    image.close()
    imageReader.close()
    renderNode.discardDisplayList()
    hardwareRenderer.destroy()

    return bitmap
}

fun Bitmap.brightness(): Float {
    var totalBrightness = 0f
    val totalPixels = this.width * this.height

    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            val pixel = this.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val brightness =
                sqrt(0.299f * red * red + 0.587f * green * green + 0.114f * blue * blue)
            totalBrightness += brightness
        }
    }

    return totalBrightness / totalPixels
}

fun Float.colorMatrix(): ColorMatrix {
    val colorMatrix = ColorMatrix()
    val adjustment = when {
        this < 50 -> 40f
        this < 100 -> 20f
        this > 200 -> -40f
        this > 150 -> -20f
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
