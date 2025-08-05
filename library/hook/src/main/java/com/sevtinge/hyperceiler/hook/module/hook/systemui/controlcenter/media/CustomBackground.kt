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

import android.app.WallpaperColors
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.conColorScheme2
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.conColorScheme3
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.defaultColorConfig
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.enumStyleContent
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.fldColorSchemeAccent1
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.fldColorSchemeAccent2
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.fldColorSchemeNeutral1
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.fldColorSchemeNeutral2
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.fldTonalPaletteAllShades
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.getScaledBackground
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory.getWallpaperColor
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.playerTwoCircleView
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.drawable.MediaControlBgDrawable
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.BgProcessor
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.BlurredCoverProcessor
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.CoverArtProcessor
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.LinearGradientProcessor
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.MediaViewColorConfig
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.RadialGradientProcessor
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.hook.utils.getValueByField
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// https://github.com/HowieHChen/XiaomiHelper/blob/72d6a928358f7de7a3b3e872f18acaa83f1cfe33/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/CustomBackground.kt
object CustomBackground : BaseHook() {
    // background:
    // 0 -> Default;
    // 1 -> Art;
    // 2 -> Blurred cover;
    // 3 -> AndroidNewStyle;
    // 4 -> AndroidOldStyle
    // 5 -> HyperOS Blur
    private val backgroundStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0)
    }
    private lateinit var processor: BgProcessor

    private var mArtworkBoundId = 0
    private var mArtworkNextBindRequestId = 0
    private var mArtworkDrawable: MediaControlBgDrawable? = null
    private var mIsArtworkBound = false
    private var mCurrentPkgName = ""

    private var mPrevColorConfig = defaultColorConfig
    private var mCurrColorConfig = defaultColorConfig

    private var lastWidth = 0
    private var lastHeight = 0

    private val isAndroidB by lazy {
        isMoreAndroidVersion(36)
    }
    private val isAndroidV by lazy {
        isAndroidVersion(35)
    }

    override fun init() {
        processor = when (backgroundStyle) {
            1 -> CoverArtProcessor()
            2 -> BlurredCoverProcessor()
            3 -> RadialGradientProcessor()
            4 -> LinearGradientProcessor()
            else -> return
        }

        if (!isAndroidB) {
            if (isAndroidV) {
                loadClassOrNull("com.android.systemui.media.controls.ui.controller.MediaViewController")!!.methodFinder()
                    .filterByName("resetLayoutResource")
                    .first()
                    .replaceMethod {
                        null
                    }
            }

            playerTwoCircleView?.apply {
                if (isAndroidV) {
                    constructorFinder().filterByParamCount(4)
                } else {
                    constructorFinder().filterByParamCount(3)
                }.first().createAfterHook { param ->
                    param.thisObject.getObjectFieldOrNullAs<Paint>("mPaint1")?.alpha = 0
                    param.thisObject.getObjectFieldOrNullAs<Paint>("mPaint2")?.alpha = 0
                    param.thisObject.setObjectField("mRadius", 0.0f)

                }

                methodFinder().filterByName("setBackground")
                    .first()
                    .createHook {
                        returnConstant(null)
                    }

                methodFinder().filterByName("setPaintColor")
                    .first()
                    .createHook {
                        returnConstant(null)
                    }
            }

            miuiMediaControlPanel!!.apply {
                // com.android.systemui.media.controls.ui.MediaControlPanel
                /*superclass!!.methodFinder().filterByName("attachPlayer")
                    .first()
                    .createAfterHook { param ->
                        val mMediaViewHolder =
                            param.thisObject.getObjectField("mMediaViewHolder")
                                ?: return@createAfterHook

                        initMediaViewHolder(mMediaViewHolder)
                    }*/


                if (isAndroidV) {
                    // Android 16 在其 com.android.systemui.media.controls.ui.controller.MediaControlPanel 类可见
                    // Android 15 在其超类中抽象了此方法，并在继承中重写了此方法
                    // Android 14 查无此方法
                    methodFinder().filterByName("onDestroy")
                        .first()
                        .createAfterHook {
                            finiMediaViewHolder()
                        }

                    methodFinder().filterByName("setPlayerBg")
                        .first()
                        .replaceMethod {
                            null
                        }

                    methodFinder().filterByName("setForegroundColors")
                        .first()
                        .replaceMethod {
                            null
                        }
                }


                methodFinder().filterByName("bindPlayer")
                    .first()
                    .createAfterHook { param ->
                        val context = param.thisObject.getObjectFieldOrNullAs<Context>("mContext")
                            ?: return@createAfterHook
                        val mediaData = param.args[0] ?: return@createAfterHook
                        val artwork = mediaData.getObjectFieldOrNullAs<Icon>("artwork")
                        val packageName = mediaData.getObjectFieldOrNullAs<String>("packageName")
                            ?: return@createAfterHook
                        val isArtWorkUpdate =
                            param.thisObject.getBooleanField("mIsArtworkUpdate")
                                || mCurrentPkgName != packageName
                        val mMediaViewHolder =
                            getValueByField(param.thisObject, "mMediaViewHolder") ?: return@createAfterHook
                        val holder = initMediaViewHolder(mMediaViewHolder) ?: return@createAfterHook

                        updateBackground(context, isArtWorkUpdate, artwork, packageName, holder)
                    }

            }
        } else {
            miuiMediaViewControllerImpl?.apply {
                methodFinder().filterByName("updateMediaBackground")
                    .first()
                    .replaceMethod {
                        null
                    }

                methodFinder().filterByName("detach")
                    .first()
                    .createBeforeHook { param ->
                        finiMediaViewHolder()
                    }

                methodFinder().filterByName("updateForegroundColors")
                    .first()
                    .replaceMethod {
                        null
                    }

                methodFinder().filterByName("bindMediaData")
                    .first()
                    .createAfterHook { param ->
                        val context = param.thisObject.getObjectFieldOrNullAs<Context>("context")
                            ?: return@createAfterHook
                        val mediaData = param.args[0] ?: return@createAfterHook
                        val artwork = mediaData.getObjectFieldOrNullAs<Icon>("artwork")
                        val packageName = mediaData.getObjectFieldOrNullAs<String>("packageName")
                            ?: return@createAfterHook
                        val isArtWorkUpdate =
                            param.thisObject.getBooleanField("isArtWorkUpdate")
                                || mCurrentPkgName != packageName
                        val mMediaViewHolder =
                            getValueByField(param.thisObject, "holder") ?: return@createAfterHook
                        val holder = initMediaViewHolder(mMediaViewHolder) ?: return@createAfterHook

                        updateBackground(context, isArtWorkUpdate, artwork, packageName, holder)
                    }

            }
        }

    }

    private fun initMediaViewHolder(mMediaViewHolder: Any): MiuiMediaViewHolder? {
        val mediaBg = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("mediaBg") ?: return null
        val titleText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("titleText") ?: return null
        val artistText = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("artistText") ?: return null
        val seamlessIcon = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("seamlessIcon") ?: return null
        val action0 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action0") ?: return null
        val action1 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action1") ?: return null
        val action2 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action2") ?: return null
        val action3 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action3") ?: return null
        val action4 = mMediaViewHolder.getObjectFieldOrNullAs<ImageButton>("action4") ?: return null
        val seekBar = mMediaViewHolder.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return null
        val elapsedTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("elapsedTimeView") ?: return null
        val totalTimeView = mMediaViewHolder.getObjectFieldOrNullAs<TextView>("totalTimeView") ?: return null
        val albumView = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("albumView") ?: return null

        return MiuiMediaViewHolder(
            mMediaViewHolder.hashCode(),
            titleText,
            artistText,
            albumView,
            mediaBg,
            seamlessIcon,
            action0,
            action1,
            action2,
            action3,
            action4,
            elapsedTimeView,
            totalTimeView,
            seekBar,
        )
    }

    private fun finiMediaViewHolder() {
        mArtworkDrawable = null
        mIsArtworkBound = false
        mCurrentPkgName = ""
    }

    private fun updateForegroundColors(holder: MiuiMediaViewHolder, colorConfig: MediaViewColorConfig) {
        val primaryColorStateList = ColorStateList.valueOf(colorConfig.textPrimary)
        holder.titleText.setTextColor(colorConfig.textPrimary)
        holder.artistText.setTextColor(colorConfig.textSecondary)
        holder.seamlessIcon.imageTintList = primaryColorStateList
        holder.action0.imageTintList = primaryColorStateList
        holder.action1.imageTintList = primaryColorStateList
        holder.action2.imageTintList = primaryColorStateList
        holder.action3.imageTintList = primaryColorStateList
        holder.action4.imageTintList = primaryColorStateList
        holder.seekBar.thumb.setTintList(primaryColorStateList)
        holder.seekBar.progressTintList = primaryColorStateList
        holder.seekBar.progressBackgroundTintList = primaryColorStateList
        holder.elapsedTimeView.setTextColor(colorConfig.textPrimary)
        holder.totalTimeView.setTextColor(colorConfig.textPrimary)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    fun updateBackground(context: Context, isArtWorkUpdate: Boolean, artwork: Icon?, pkgName: String, holder: MiuiMediaViewHolder) {
        val artworkLayer = artwork?.loadDrawable(context) ?: return
        val reqId = mArtworkNextBindRequestId++
        if (isArtWorkUpdate) {
            mIsArtworkBound = false
        }
        // Clip album cover image
//        val finalSize = min(artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight)
//        val bitmap = createBitmap(finalSize, finalSize)
//        val canvas = Canvas(bitmap)
//        val deltaW = (artworkLayer.intrinsicWidth - finalSize) / 2
//        val deltaH = (artworkLayer.intrinsicHeight - finalSize) / 2
//        artworkLayer.setBounds(-deltaW, -deltaH, finalSize + deltaW, finalSize + deltaH)
//        artworkLayer.draw(canvas)
//        val radius = 9.0f * context.resources.displayMetrics.density
//        val newBitmap = createBitmap(finalSize, finalSize)
//        val canvas1 = Canvas(newBitmap)
//        val paint = Paint()
//        val rect = Rect(0, 0, finalSize, finalSize)
//        val rectF = RectF(rect)
//        paint.isAntiAlias = true
//        canvas1.drawARGB(0, 0, 0, 0)
//        paint.color = Color.BLACK
//        canvas1.drawRoundRect(rectF, radius, radius, paint)
//        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//        canvas1.drawBitmap(bitmap, rect, rect, paint)
//        if (!bitmap.isRecycled) {
//            bitmap.recycle()
//        }
        // Update album cover image
        holder.albumView.setImageDrawable(artworkLayer)
        // Capture width & height from views in foreground for artwork scaling in background
        val width: Int
        val height: Int
        if (holder.mediaBg.measuredWidth == 0 || holder.mediaBg.measuredHeight == 0) {
            if (lastWidth == 0 || lastHeight == 0) {
                width = artworkLayer.intrinsicWidth
                height = artworkLayer.intrinsicHeight
            } else {
                width = lastWidth
                height =
                    lastHeight
            }
        } else {
            width = holder.mediaBg.measuredWidth
            height = holder.mediaBg.measuredHeight
            lastWidth = width
            lastHeight = height
        }
        // Override colors set by the original method
        updateForegroundColors(holder, mCurrColorConfig)

        GlobalScope.launch(Dispatchers.IO) {
            // Album art
            val mutableColorScheme: Any?
            val artworkDrawable: Drawable
            val isArtworkBound: Boolean
            val wallpaperColors = context.getWallpaperColor(artwork)
            if (wallpaperColors != null) {
                val tempColorScheme = try {
                    conColorScheme3.newInstance(wallpaperColors, true, enumStyleContent)
                } catch (_: IllegalArgumentException) {
                    conColorScheme2.newInstance(wallpaperColors, enumStyleContent)
                }
                mutableColorScheme = tempColorScheme
                artworkDrawable = context.getScaledBackground(artwork, height, height) ?: Color.TRANSPARENT.toDrawable()
                isArtworkBound = true
            } else {
                // If there's no artwork, use colors from the app icon
                artworkDrawable = Color.TRANSPARENT.toDrawable()
                isArtworkBound = false
                try {
                    val icon = context.packageManager.getApplicationIcon(pkgName)
                    val tempColorScheme = try {
                        conColorScheme3.newInstance(WallpaperColors.fromDrawable(icon), true, enumStyleContent)
                    } catch (_: IllegalArgumentException) {
                        conColorScheme2.newInstance(wallpaperColors, enumStyleContent)
                    }
                    mutableColorScheme = tempColorScheme ?: throw Exception()
                } catch (_: Exception) {
                    logW(TAG, lpparam.packageName, "updateBackground(method) application not found!")
                    return@launch
                }
            }
            var colorConfig = defaultColorConfig
            var colorSchemeChanged = false
            if (mutableColorScheme != null) {
                val neutral1 = fldTonalPaletteAllShades?.get(fldColorSchemeNeutral1!!.get(mutableColorScheme)) as? List<Int>
                val neutral2 = fldTonalPaletteAllShades?.get(fldColorSchemeNeutral2!!.get(mutableColorScheme)) as? List<Int>
                val accent1 = fldTonalPaletteAllShades?.get(fldColorSchemeAccent1!!.get(mutableColorScheme)) as? List<Int>
                val accent2 = fldTonalPaletteAllShades?.get(fldColorSchemeAccent2!!.get(mutableColorScheme)) as? List<Int>
                if (neutral1 != null && neutral2 != null && accent1 != null && accent2 != null) {
                    colorConfig = processor.convertToColorConfig(artworkDrawable, neutral1, neutral2, accent1, accent2)
                    colorSchemeChanged = colorConfig != mPrevColorConfig
                    mPrevColorConfig = colorConfig
                }
            }
            val processedArtwork =
                processor.processAlbumCover(
                    artworkDrawable,
                    colorConfig,
                    context,
                    width,
                    height
                )
            if (mArtworkDrawable == null) {
                mArtworkDrawable = processor.createBackground(processedArtwork, colorConfig)
            }
            mArtworkDrawable?.setBounds(0, 0, width, height)
            mCurrentPkgName = pkgName

            holder.mediaBg.post(Runnable {
                if (reqId < mArtworkBoundId) {
                    return@Runnable
                }
                mArtworkBoundId = reqId
                if (colorSchemeChanged) {
                    updateForegroundColors(holder, colorConfig)
                    mCurrColorConfig = colorConfig
                }

                // Bind the album view to the artwork or a transition drawable
                holder.mediaBg.setPadding(0, 0, 0, 0)
                if (isArtWorkUpdate || (!mIsArtworkBound && isArtworkBound)) {
                    holder.mediaBg.setImageDrawable(mArtworkDrawable)
                    mArtworkDrawable?.updateAlbumCover(processedArtwork, colorConfig)
                    mIsArtworkBound = isArtworkBound
                }
            })
        }
    }

    data class MiuiMediaViewHolder(
        var innerHashCode: Int,
        var titleText: TextView,
        var artistText: TextView,
        var albumView: ImageView,
        var mediaBg: ImageView,
        var seamlessIcon: ImageView,
        var action0: ImageButton,
        var action1: ImageButton,
        var action2: ImageButton,
        var action3: ImageButton,
        var action4: ImageButton,
        var elapsedTimeView: TextView,
        var totalTimeView: TextView,
        var seekBar: SeekBar
    )

}
