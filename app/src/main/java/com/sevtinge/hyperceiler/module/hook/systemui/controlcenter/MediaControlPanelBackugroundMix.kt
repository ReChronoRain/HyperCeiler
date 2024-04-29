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

import android.annotation.*
import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.hardware.*
import android.media.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiBackgroundBlendColors
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*
import kotlin.math.*
import kotlin.random.*

private var artwork: Icon? = null

//from https://github.com/YuKongA/MediaControl-BlurBg/blob/752de17a31d940683648cee7b957d4ff48d381a3/app/src/main/kotlin/top/yukonga/mediaControlBlur/MainHook.kt
class MediaControlPanelBackgroundMix : BaseHook() {
    private val radius by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_panel_background_mix_blur_radius", 40)
    }
    private val overlay by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_panel_background_mix_overlay", 20)
    }
    override fun init() {
        // 部分代码来自 Hyper Helper (https://github.com/HowieHChen/XiaomiHelper/blob/master/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/CustomMusicControl.kt)
        val mediaControlPanel = loadClassOrNull("com.android.systemui.media.controls.ui.MediaControlPanel")
        val miuiMediaControlPanel = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
        val notificationUtil = loadClassOrNull("com.android.systemui.statusbar.notification.NotificationUtil")
        val playerTwoCircleView = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.PlayerTwoCircleView")

        if (mPrefsMap.getBoolean("system_ui_control_center_remove_media_control_panel_background")) {
            removeBackground(mediaControlPanel, notificationUtil, miuiMediaControlPanel, playerTwoCircleView)
        } else {
            setBlurBackground(miuiMediaControlPanel, playerTwoCircleView)
        }
    }

    private fun removeBackground(
        mediaControlPanel: Class<*>?,
        notificationUtil: Class<*>?,
        miuiMediaControlPanel: Class<*>?,
        playerTwoCircleView: Class<*>?
    ) {
        try {
            mediaControlPanel?.methodFinder()?.filterByName("attachPlayer")?.first()?.createAfterHook {
                val context = AndroidAppHelper.currentApplication().applicationContext

                val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                if (!isBackgroundBlurOpened) return@createAfterHook

                val mMediaViewHolder = it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder") ?: return@createAfterHook
                val mediaBg = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("mediaBg") ?: return@createAfterHook

                mediaBg.apply {
                    setMiViewBlurMode(1)
                    setMiBackgroundBlurRadius(radius)
                    setBlurRoundRect(getNotificationElementRoundRect(context))
                    setMiBackgroundBlendColors(getNotificationElementBlendColors(context), 1f)
                }
            }

            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
                ?.createAfterHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext

                    val isBackgroundBlurOpened =
                        XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean

                    val mMediaViewHolder =
                        it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder") ?: return@createAfterHook

                    val mediaBg =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("mediaBg") ?: return@createAfterHook
                    val titleText =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("titleText")
                    val artistText =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("artistText")
                    val seamlessIcon =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("seamlessIcon")
                    val action0 =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action0")
                    val action1 =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action1")
                    val action2 =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action2")
                    val action3 =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action3")
                    val action4 =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action4")
                    val seekBar =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<SeekBar>("seekBar")
                    val elapsedTimeView =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("elapsedTimeView")
                    val totalTimeView =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("totalTimeView")
                    val albumView =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("albumView")
                    val appIcon =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("appIcon")

                    val grey =
                        if (isDarkMode()) Color.parseColor("#80ffffff") else Color.parseColor("#99000000")

                    if (!isBackgroundBlurOpened) {
                        titleText?.setTextColor(Color.WHITE)
                        seamlessIcon?.setColorFilter(Color.WHITE)
                        action0?.setColorFilter(Color.WHITE)
                        action1?.setColorFilter(Color.WHITE)
                        action2?.setColorFilter(Color.WHITE)
                        action3?.setColorFilter(Color.WHITE)
                        action4?.setColorFilter(Color.WHITE)
                        seekBar?.progressDrawable?.colorFilter = colorFilter(Color.WHITE)
                        seekBar?.thumb?.colorFilter = colorFilter(Color.WHITE)
                    } else {
                        if (!isDarkMode()) {
                            titleText?.setTextColor(Color.BLACK)
                            artistText?.setTextColor(grey)
                            seamlessIcon?.setColorFilter(Color.BLACK)
                            action0?.setColorFilter(Color.BLACK)
                            action1?.setColorFilter(Color.BLACK)
                            action2?.setColorFilter(Color.BLACK)
                            action3?.setColorFilter(Color.BLACK)
                            action4?.setColorFilter(Color.BLACK)
                            seekBar?.progressDrawable?.colorFilter = colorFilter(Color.BLACK)
                            seekBar?.thumb?.colorFilter = colorFilter(if (mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2) Color.TRANSPARENT else Color.BLACK)
                            elapsedTimeView?.setTextColor(grey)
                            totalTimeView?.setTextColor(grey)
                        } else {
                            titleText?.setTextColor(Color.WHITE)
                            artistText?.setTextColor(grey)
                            seamlessIcon?.setColorFilter(Color.WHITE)
                            action0?.setColorFilter(Color.WHITE)
                            action1?.setColorFilter(Color.WHITE)
                            action2?.setColorFilter(Color.WHITE)
                            action3?.setColorFilter(Color.WHITE)
                            action4?.setColorFilter(Color.WHITE)
                            seekBar?.progressDrawable?.colorFilter = colorFilter(Color.WHITE)
                            seekBar?.thumb?.colorFilter = colorFilter((if (mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2) Color.TRANSPARENT else Color.WHITE))
                            elapsedTimeView?.setTextColor(grey)
                            totalTimeView?.setTextColor(grey)
                        }

                        mediaBg.setMiBackgroundBlendColors(getNotificationElementBlendColors(context), 1f)

                        val artwork = it.args[0].objectHelper().getObjectOrNullAs<Icon>("artwork") ?: return@createAfterHook
                        val artworkLayer = artwork.loadDrawable(context) ?: return@createAfterHook
                        val artworkBitmap = Bitmap.createBitmap(
                            artworkLayer.intrinsicWidth,
                            artworkLayer.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(artworkBitmap)
                        artworkLayer.setBounds(0, 0, artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight)
                        artworkLayer.draw(canvas)
                        val resizedBitmap = Bitmap.createScaledBitmap(artworkBitmap, 300, 300, true)

                        val radius = 45f
                        val newBitmap = Bitmap.createBitmap(
                            resizedBitmap.width,
                            resizedBitmap.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas1 = Canvas(newBitmap)

                        val paint = Paint()
                        val rect = Rect(0, 0, resizedBitmap.width, resizedBitmap.height)
                        val rectF = RectF(rect)

                        paint.isAntiAlias = true
                        canvas1.drawARGB(0, 0, 0, 0)
                        paint.color = Color.BLACK
                        canvas1.drawRoundRect(rectF, radius, radius, paint)

                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        canvas1.drawBitmap(resizedBitmap, rect, rect, paint)

                        albumView?.setImageDrawable(BitmapDrawable(context.resources, newBitmap))

                        if (appIcon?.parent != null) {
                            (appIcon.parent as ViewGroup?)?.removeView(appIcon)
                        }
                    }
                }

            playerTwoCircleView?.methodFinder()?.filterByName("onDraw")?.first()?.createBeforeHook {
                val context = AndroidAppHelper.currentApplication().applicationContext

                val isBackgroundBlurOpened =
                    XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                if (!isBackgroundBlurOpened) return@createBeforeHook

                it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint1")?.alpha = 0
                it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint2")?.alpha = 0
                it.thisObject.objectHelper().setObject("mRadius", 0f)
            }

            playerTwoCircleView?.methodFinder()?.filterByName("setBackground")?.first()
                ?.createBeforeHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext

                val isBackgroundBlurOpened =
                    XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context) as Boolean
                if (!isBackgroundBlurOpened) return@createBeforeHook
                    it.result = null
                }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }

    private fun setBlurBackground(
        miuiMediaControlPanel: Class<*>?,
        playerTwoCircleView: Class<*>?
    ) {
        try {
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
                        val backgroundColor = Color.argb(
                            overlay, backgroundColorMode, backgroundColorMode, backgroundColorMode
                        )

                        // 应用颜色过滤器
                        val paintOverlay = Paint()
                        paintOverlay.colorFilter =
                            PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP)

                        // 叠加颜色
                        canvas2.drawBitmap(bigBitmap, 0f, 0f, null)
                        canvas2.drawColor(backgroundColor)

                        // 模糊处理
                        artworkLayer = BitmapDrawable(
                            imageView.resources, bigBitmap.blur(
                                radius.toFloat()
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

    @SuppressLint("DiscouragedApi")
    fun getNotificationElementBlendColors(context: Context): IntArray {
        val resources = context.resources
        return try {
            val arrayId = resources.getIdentifier("notification_element_blend_shade_colors", "array", "com.android.systemui")
            resources.getIntArray(arrayId)
        } catch (_: Exception) {
            val arrayId = resources.getIdentifier("notification_element_blend_colors", "array", "com.android.systemui")
            resources.getIntArray(arrayId)
        }
    }

    @SuppressLint("DiscouragedApi")
    fun getNotificationElementRoundRect(context: Context): Int {
        val resources = context.resources
        val dimenId = resources.getIdentifier("notification_item_bg_radius", "dimen", "com.android.systemui")
        return resources.getDimensionPixelSize(dimenId)
    }
}

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
