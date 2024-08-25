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
import android.content.res.*
import android.content.res.Resources.*
import android.database.*
import android.graphics.*
import android.graphics.drawable.*
import android.hardware.*
import android.media.*
import android.os.*
import android.provider.*
import android.util.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiBackgroundBlendColors
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*
import kotlin.math.*
import kotlin.random.*

private var artwork: Icon? = null

// from https://github.com/YuKongA/MediaControl-BlurBg/blob/752de17a31d940683648cee7b957d4ff48d381a3/app/src/main/kotlin/top/yukonga/mediaControlBlur/MainHook.kt
class MediaControlPanelBackgroundMix : BaseHook() {
    private val radius by lazy {
        mPrefsMap.getInt(
            "system_ui_control_center_media_control_panel_background_mix_blur_radius",
            40
        )
    }
    private val overlay by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_panel_background_mix_overlay", 20)
    }
    val Int.dp: Int get() = (this.toFloat().dp).toInt()
    val Float.dp: Float get() = this / getSystem().displayMetrics.density
    override fun init() {
        // 部分代码来自 Hyper Helper (https://github.com/HowieHChen/XiaomiHelper/blob/master/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/CustomMusicControl.kt)
        val miuiMediaControlPanel = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
        val notificationUtil = loadClassOrNull("com.android.systemui.statusbar.notification.NotificationUtil")
        val playerTwoCircleView = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.PlayerTwoCircleView")
        val seekBarObserver = loadClassOrNull("com.android.systemui.media.controls.models.player.SeekBarObserver")
        val mediaViewHolder = loadClassOrNull("com.android.systemui.media.controls.models.player.MediaViewHolder")
        val statusBarStateControllerImpl = loadClassOrNull("com.android.systemui.statusbar.StatusBarStateControllerImpl")

        mediaViewHolder?.constructors?.first()?.createAfterHook {
            val context = AndroidAppHelper.currentApplication().applicationContext
            val action0 = it.thisObject.objectHelper().getObjectOrNullAs<ImageButton>("action0")
            val action1 = it.thisObject.objectHelper().getObjectOrNullAs<ImageButton>("action1")
            val action2 = it.thisObject.objectHelper().getObjectOrNullAs<ImageButton>("action2")
            val action3 = it.thisObject.objectHelper().getObjectOrNullAs<ImageButton>("action3")
            val action4 = it.thisObject.objectHelper().getObjectOrNullAs<ImageButton>("action4")

            fun updateColorFilter() {
                val color = if (isDarkMode()) Color.WHITE else Color.BLACK
                action0?.setColorFilter(color)
                action1?.setColorFilter(color)
                action2?.setColorFilter(color)
                action3?.setColorFilter(color)
                action4?.setColorFilter(color)
            }

            updateColorFilter()

            val darkModeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    updateColorFilter()
                }
            }

            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("ui_night_mode"), false, darkModeObserver
            )
        }

        if (mPrefsMap.getBoolean("system_ui_control_center_remove_media_control_panel_background")) {
            removeBackground(notificationUtil, miuiMediaControlPanel, playerTwoCircleView, seekBarObserver, statusBarStateControllerImpl)
        } else {
            setBlurBackground(miuiMediaControlPanel, playerTwoCircleView)
        }
    }

    // https://github.com/YuKongA/MediaControl-BlurBg/commit/1362b32a59de2483d83221e859548d330701fab1
    private fun removeBackground(
        notificationUtil: Class<*>?,
        miuiMediaControlPanel: Class<*>?,
        playerTwoCircleView: Class<*>?,
        seekBarObserver: Class<*>?,
        statusBarStateControllerImpl: Class<*>?
    ) {
        try {
            var lockScreenStatus: Boolean? = null
            var darkModeStatus: Boolean? = null
            // 这里不能hook，一hook圆角就丢，我也不知道为什么，yukonga那里就没问题，但是他既然已经跑起来了我就不动他了
            /*seekBarObserver?.constructors?.first()?.createAfterHook {
                it.thisObject.objectHelper().setObject("seekBarEnabledMaxHeight", 9.dp)
                val seekBar = it.args[0].objectHelper().getObjectOrNullAs<SeekBar>("seekBar")
                val backgroundDrawable = GradientDrawable().apply {
                    color = ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor("#20ffffff")))
                    cornerRadius = 9.dp.toFloat()
                }

                val onProgressDrawable = GradientDrawable().apply {
                    color = ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor("#ffffffff")))
                    cornerRadius = 9.dp.toFloat()
                }

                val thumbDrawable = seekBar?.thumb as LayerDrawable
                val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, ClipDrawable(onProgressDrawable, Gravity.START, ClipDrawable.HORIZONTAL)))

                seekBar.apply {
                    thumb = thumbDrawable
                    progressDrawable = layerDrawable
                }
            }*/

            miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
                ?.createAfterHook {
                    val context = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<Context>("mContext") ?: return@createAfterHook
                    val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(
                        notificationUtil,
                        "isBackgroundBlurOpened",
                        context
                    ) as Boolean
                    val mMediaViewHolder = it.thisObject.objectHelper()
                        .getObjectOrNullUntilSuperclass("mMediaViewHolder")
                        ?: return@createAfterHook
                    val titleText =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("titleText")
                    val artistText =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("artistText")
                    val seamlessIcon =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("seamlessIcon")
                    val seekBar =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<SeekBar>("seekBar")
                    val elapsedTimeView = mMediaViewHolder.objectHelper()
                        .getObjectOrNullAs<TextView>("elapsedTimeView")
                    val totalTimeView =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("totalTimeView")
                    val albumView =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("albumView")
                    val appIcon =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("appIcon")

                    val artwork = it.args[0].objectHelper().getObjectOrNullAs<Icon>("artwork") ?: return@createAfterHook
                    val artworkLayer = artwork.loadDrawable(context) ?: return@createAfterHook

                    val artworkBitmap = Bitmap.createBitmap(artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(artworkBitmap)
                    artworkLayer.setBounds(0, 0, artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight)
                    artworkLayer.draw(canvas)
                    val resizedBitmap = Bitmap.createScaledBitmap(artworkBitmap, 300, 300, true)
                    val radius = 45f
                    val newBitmap = Bitmap.createBitmap(resizedBitmap.width, resizedBitmap.height, Bitmap.Config.ARGB_8888)
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
                    (appIcon?.parent as ViewGroup?)?.removeView(appIcon)

                    val grey = if (isDarkMode()) Color.LTGRAY else Color.DKGRAY
                    val color = if (isDarkMode()) Color.WHITE else Color.BLACK
                    seekBar?.thumb?.colorFilter = colorFilter(Color.TRANSPARENT)
                    elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                    totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                    if (!isBackgroundBlurOpened) {
                        titleText?.setTextColor(Color.WHITE)
                        seamlessIcon?.setColorFilter(Color.WHITE)
                        seekBar?.progressDrawable?.colorFilter = colorFilter(Color.WHITE)
                        seekBar?.thumb?.colorFilter = colorFilter(Color.WHITE)
                    } else {
                        artistText?.setTextColor(grey)
                        elapsedTimeView?.setTextColor(grey)
                        totalTimeView?.setTextColor(grey)
                        titleText?.setTextColor(Color.WHITE)
                        seamlessIcon?.setColorFilter(Color.WHITE)
                        seekBar?.progressDrawable?.colorFilter = colorFilter(Color.WHITE)
                        seekBar?.thumb?.colorFilter = colorFilter(if (mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 2) Color.TRANSPARENT else Color.WHITE)
                        /*if (!isDarkMode()) {
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
                        }*/

                        /*val artwork =
                            it.args[0].objectHelper().getObjectOrNullAs<Icon>("artwork")
                                ?: return@createAfterHook
                        val artworkLayer =
                            artwork.loadDrawable(context) ?: return@createAfterHook
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
                        val resizedBitmap =
                            Bitmap.createScaledBitmap(artworkBitmap, 300, 300, true)
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
                        albumView?.setImageDrawable(
                            BitmapDrawable(
                                context.resources,
                                newBitmap
                            )
                        )
                        (appIcon?.parent as ViewGroup?)?.removeView(appIcon)
                        elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                        totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)*/
                    }
                }

            playerTwoCircleView?.methodFinder()?.filterByName("onDraw")?.first()
                ?.createBeforeHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext

                    val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(
                        notificationUtil,
                        "isBackgroundBlurOpened",
                        context
                    ) as Boolean
                    if (!isBackgroundBlurOpened) return@createBeforeHook

                    val mPaint1 = it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint1")
                    val mPaint2 = it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint2")
                    if (mPaint1?.alpha == 0) return@createBeforeHook

                    mPaint1?.alpha = 0
                    mPaint2?.alpha = 0
                    it.thisObject.objectHelper().setObject("mRadius", 0f)

                    (it.thisObject as ImageView).setMiViewBlurMode(1)
                    (it.thisObject as ImageView).setBlurRoundRect(
                        getNotificationElementRoundRect(context)
                    )

                    val miuiStubClass = loadClassOrNull("miui.stub.MiuiStub")
                    val miuiStubInstance = XposedHelpers.getStaticObjectField(miuiStubClass, "INSTANCE")
                    val mSysUIProvider = XposedHelpers.getObjectField(miuiStubInstance, "mSysUIProvider")
                    val mStatusBarStateController = XposedHelpers.getObjectField(mSysUIProvider, "mStatusBarStateController")
                    val getLazyClass = XposedHelpers.callMethod(mStatusBarStateController, "get")
                    val getState = XposedHelpers.callMethod(getLazyClass, "getState")

                    (it.thisObject as ImageView).apply {
                        getNotificationElementBlendColors(context, getState == 1)?.let { iArr -> setMiBackgroundBlendColors(iArr, 1f) }
                    }

                    statusBarStateControllerImpl?.methodFinder()?.filterByName("getState")
                        ?.first()?.createAfterHook { hookParam1 ->
                            val getStatusBarState = hookParam1.result as Int
                            val isInLockScreen = getStatusBarState == 1
                            val isDarkMode = isDarkMode()
                            if (lockScreenStatus == null || darkModeStatus == null || lockScreenStatus != isInLockScreen || darkModeStatus != isDarkMode) {                                lockScreenStatus = isInLockScreen
                                darkModeStatus = isDarkMode
                                (it.thisObject as ImageView).apply {
                                    getNotificationElementBlendColors(
                                        context,
                                        isInLockScreen
                                    )?.let { iArr -> setMiBackgroundBlendColors(iArr, 1f) }
                                }
                            }
                        }
                }

            playerTwoCircleView?.methodFinder()?.filterByName("setBackground")?.first()
                ?.createBeforeHook {
                    val context = AndroidAppHelper.currentApplication().applicationContext
                    val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(
                        notificationUtil,
                        "isBackgroundBlurOpened",
                        context
                    ) as Boolean
                    if (!isBackgroundBlurOpened) return@createBeforeHook
                    it.result = null
                }
        } catch (t: Throwable) {
           logE(TAG, lpparam.packageName, t)
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
            logE(TAG, lpparam.packageName, t)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getNotificationElementBlendColors(context: Context, isInLockScreen: Boolean): IntArray? {
        val resources = context.resources
        val theme = context.theme
        var arrayInt: IntArray? = null
        try {
            if (isInLockScreen) {
                val arrayId = resources.getIdentifier("notification_element_blend_keyguard_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            } else {
                val arrayId = resources.getIdentifier("notification_element_blend_shade_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            }
            return arrayInt
        } catch (_: Exception) {
        }

        try {
            if (isInLockScreen) {
                val color1 = getResourceValue(resources, "notification_element_blend_keyguard_color_1", "color", theme)
                val color2 = getResourceValue(resources, "notification_element_blend_keyguard_color_2", "color", theme)
                val integer1 = getResourceValue(resources, "notification_element_blend_keyguard_mode_1", "integer")
                val integer2 = getResourceValue(resources, "notification_element_blend_keyguard_mode_2", "integer")
                arrayInt = intArrayOf(color1, integer1, color2, integer2)
            } else {
                val color1 = getResourceValue(resources, "notification_element_blend_shade_color_1", "color", theme)
                val color2 = getResourceValue(resources, "notification_element_blend_shade_color_2", "color", theme)
                val integer1 = getResourceValue(resources, "notification_element_blend_shade_mode_1", "integer")
                val integer2 = getResourceValue(resources, "notification_element_blend_shade_mode_2", "integer")
                arrayInt = intArrayOf(color1, integer1, color2, integer2)
            }
            return arrayInt
        } catch (_: Exception) {
        }

        try {
            if (isInLockScreen) {
                val arrayId = resources.getIdentifier("notification_element_keyguard_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId)
            } else {
                val arrayId = resources.getIdentifier("notification_element_blend_colors", "array", "com.android.systemui")
                arrayInt = resources.getIntArray(arrayId) }
            return arrayInt
        } catch (_: Exception) {
        }

        return arrayInt
    }

    @SuppressLint("DiscouragedApi")
    private fun getResourceValue(resources: Resources, name: String, type: String, theme: Resources.Theme? = null): Int {
        val id = resources.getIdentifier(name, type, "com.android.systemui")
        return when (type) {
            "color" -> resources.getColor(id, theme)
            "integer" -> resources.getInteger(id)
            else -> throw IllegalArgumentException("Unsupported resource type: $type")
        }
    }

    @SuppressLint("DiscouragedApi")
    fun getNotificationElementRoundRect(context: Context): Int {
        val resources = context.resources
        val dimenId =
            resources.getIdentifier("notification_item_bg_radius", "dimen", "com.android.systemui")
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
