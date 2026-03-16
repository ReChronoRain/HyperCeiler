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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.app.WallpaperColors
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion
import com.sevtinge.hyperceiler.libhook.utils.api.HostExecutor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.defaultColorConfig
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldColorSchemeAccent1
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldColorSchemeAccent2
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldColorSchemeNeutral1
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldColorSchemeNeutral2
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldTonalPaletteAllShades
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.getCachedWallpaperColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.getScaledBackground
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.newColorScheme
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.releaseCachedWallpaperColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.clzConstraintSetClass
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.hyperProgressSeekBar
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaData
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewBinderImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MiuiMediaViewHolderWrapper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.PlayerConfig
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.PlayerType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.applyTo
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.clone
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.connect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.getMediaViewHolderFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.setVisibility
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg.BgProcessor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg.BlurredCoverProcessor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg.CoverArtProcessor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg.LinearGradientProcessor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.mediabg.RadialGradientProcessor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getBooleanField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getDimenByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getValueByField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object CustomBackground : BaseHook() {
    private const val KEY_VIEW_HOLDER_WRAPPER = "KEY_VIEW_HOLDER_WRAPPER"
    private const val KEY_SEEKBAR_TINT_COLOR = "KEY_SEEKBAR_TINT_COLOR"
    const val KEY_REAL_PROGRESS_BAR = "KEY_REAL_PROGRESS_BAR"
    // background:
    // 0 -> Default;
    // 1 -> Art;
    // 2 -> Blurred cover;
    // 3 -> AndroidNewStyle;
    // 4 -> AndroidOldStyle
    private val ncBackgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0)
    }
    private val diBackgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_background_mode", 0)
    }

    private val mediaBgId by lazy {
        appContext.getIdByName("media_bg")
    }
    private val mediaBgViewId by lazy {
        appContext.getIdByName("media_bg_view")
    }
    private val mediaBgRadiusDi by lazy {
        appContext.getDimenByName("media_control_bg_radius")
    }

    private var ncProcessor: BgProcessor? = null
    private var diProcessor: BgProcessor? = null

    private val ncPlayerConfig = PlayerConfig()
    private val diPlayerConfig = PlayerConfig()
    private val diPlayerConfigDummy = PlayerConfig()

    private val fldArtwork by lazy {
        mediaData?.findField("artwork")
    }
    private val fldPackageName by lazy {
        mediaData?.findField("packageName")
    }

    val isIsland by lazy {
        (isMoreSmallVersion(300, 3f) && isPad()) || !isPad()
    }

    override fun init() {
        if (ncBackgroundStyle != 0) {
            ncProcessor = setBackground(ncBackgroundStyle)
            initNCForOS3()
        }
        if (isIsland && diBackgroundStyle != 0) {
            diProcessor = setBackground(diBackgroundStyle)
            initDynamicIsland()
        }
        if (hyperProgressSeekBar != null) {
            hookHyperProgressSeekBar()
        }
    }

    private fun setBackground(style: Int): BgProcessor? {
        return when (style) {
            1 -> CoverArtProcessor()
            2 -> BlurredCoverProcessor()
            3 -> RadialGradientProcessor()
            4 -> LinearGradientProcessor()
            else -> null
        }
    }

    private fun hookHyperProgressSeekBar() {
        hyperProgressSeekBar?.beforeHookMethod("onDraw") { param ->
            val tintColor = param.thisObject.getAdditionalInstanceFieldAs<Int?>(KEY_SEEKBAR_TINT_COLOR)
                ?: return@beforeHookMethod
            val mPaint = param.thisObject.getObjectFieldOrNullAs<Paint?>("mPaint") ?: return@beforeHookMethod
            mPaint.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)

            val progressDrawable = param.thisObject.getObjectFieldOrNullAs<Drawable?>("mProgressDrawable")
            progressDrawable?.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)

            val bgDrawable = param.thisObject.getObjectFieldOrNullAs<Drawable?>("mBackgroundDrawable")
            bgDrawable?.colorFilter = PorterDuffColorFilter(
                (tintColor and 0x00FFFFFF) or (0x33 shl 24),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    // ==================== OS3 通知中心 ====================

    private fun initNCForOS3() {
        val controllerClass = miuiMediaViewControllerImpl ?: return

        controllerClass.apply {
            replaceMethod("updateMediaBackground") { null }
            replaceMethod("updateForegroundColors") { null }

            beforeHookMethod("detach") { finiPlayerConfig(PlayerType.NOTIFICATION_CENTER) }

            afterHookMethod("bindMediaData") { param ->
                val context = param.thisObject.getObjectFieldOrNullAs<Context>("context")
                    ?: return@afterHookMethod
                val mediaData = param.args[0] ?: return@afterHookMethod
                val artwork = fldArtwork?.get(mediaData) as? Icon ?: return@afterHookMethod
                val packageName = fldPackageName?.get(mediaData) as? String ?: return@afterHookMethod
                val isArtWorkUpdate = param.thisObject.getBooleanField("isArtWorkUpdate")
                    || ncPlayerConfig.currentPkgName != packageName
                val mMediaViewHolder = getValueByField(param.thisObject, "holder")
                    ?: return@afterHookMethod
                val holder = wrapViewHolder(mMediaViewHolder, false) ?: return@afterHookMethod

                if (isArtWorkUpdate || !ncPlayerConfig.isArtworkBound) {
                    updateBackground(context, artwork, packageName, holder, PlayerType.NOTIFICATION_CENTER, ncProcessor)
                }
            }
        }
    }

    // ==================== OS3 灵动岛 ====================

    private fun initDynamicIsland() {
        miuiIslandMediaViewBinderImpl!!.apply {
            replaceMethod("updateForegroundColors") { null }

            afterHookMethod("detach") { finiPlayerConfig(PlayerType.DYNAMIC_ISLAND) }

            afterHookMethod("attach") { param ->
                param.thisObject.getObjectFieldOrNull("holder")?.let { wrapViewHolder(it, true) }
                param.thisObject.getObjectFieldOrNull("dummyHolder")?.let { wrapViewHolder(it, true) }
            }

            afterHookMethod("bindMediaData") { param ->
                val context = param.thisObject.getObjectFieldOrNullAs<Context>("context")
                    ?: return@afterHookMethod
                val mediaData = param.args[0] ?: return@afterHookMethod
                val artwork = fldArtwork?.get(mediaData) as? Icon ?: return@afterHookMethod
                val packageName = fldPackageName?.get(mediaData) as? String ?: return@afterHookMethod
                val holder = param.thisObject.getObjectFieldOrNull("holder")
                    ?.let { wrapViewHolder(it, true) } ?: return@afterHookMethod
                val dummyHolder = param.thisObject.getObjectFieldOrNull("dummyHolder")
                    ?.let { wrapViewHolder(it, true) } ?: return@afterHookMethod
                val isArtWorkUpdate = param.thisObject.getBooleanField("isArtWorkUpdate")
                    || diPlayerConfig.currentPkgName != packageName

                if (isArtWorkUpdate || !diPlayerConfig.isArtworkBound) {
                    updateBackground(context, artwork, packageName, holder, PlayerType.DYNAMIC_ISLAND, diProcessor)
                    updateBackground(context, artwork, packageName, dummyHolder, PlayerType.DUMMY_DYNAMIC_ISLAND, diProcessor)
                }

                val mediaBgTransYOffset = param.thisObject.getObjectFieldOrNullAs<Float>("mediaBgTransYOffset")
                if (mediaBgTransYOffset != null && mediaBgTransYOffset != 0.0f) {
                    val height = dummyHolder.mediaBg.height
                    if (height > 0) dummyHolder.mediaBg.scaleY = (height + mediaBgTransYOffset) / height
                }
            }
        }

        findClassIfExists(
            $$"com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewBinderImpl$attach$4$1"
        )?.afterHookMethod("emit") { param ->
            val pair = param.args[0] as? Pair<*, *> ?: return@afterHookMethod
            val action = pair.first as? String ?: return@afterHookMethod
            val data = pair.second as? Bundle
            val dummyMediaBgView = param.thisObject.getObjectFieldOrNull($$"$dummyHolder")
                ?.let { wrapViewHolder(it, true)?.mediaBg } ?: return@afterHookMethod

            when (action) {
                "pull_down_type_start" -> dummyMediaBgView.pivotY = 0.0f
                "pull_down_type_update" -> {
                    val offset = data?.getFloat("pull_down_action_offset_y", 0.0f) ?: 0.0f
                    val h = dummyMediaBgView.height
                    if (h > 0) dummyMediaBgView.scaleY = (h + offset) / h
                }
                "pull_down_type_finish" -> {}
            }
        }
    }

    // ==================== ViewHolder 包装 ====================

    private fun wrapViewHolder(mMediaViewHolder: Any, isDynamicIsland: Boolean): MiuiMediaViewHolderWrapper? {
        mMediaViewHolder.getAdditionalInstanceFieldAs<MiuiMediaViewHolderWrapper>(KEY_VIEW_HOLDER_WRAPPER)?.let {
            return it
        }
        val titleText = mMediaViewHolder.getMediaViewHolderFieldAs<TextView>("titleText", isDynamicIsland) ?: return null
        val artistText = mMediaViewHolder.getMediaViewHolderFieldAs<TextView>("artistText", isDynamicIsland) ?: return null
        val seamlessIcon = mMediaViewHolder.getMediaViewHolderFieldAs<ImageView>("seamlessIcon", isDynamicIsland) ?: return null
        val action0 = mMediaViewHolder.getMediaViewHolderFieldAs<ImageButton>("action0", isDynamicIsland) ?: return null
        val action1 = mMediaViewHolder.getMediaViewHolderFieldAs<ImageButton>("action1", isDynamicIsland) ?: return null
        val action2 = mMediaViewHolder.getMediaViewHolderFieldAs<ImageButton>("action2", isDynamicIsland) ?: return null
        val action3 = mMediaViewHolder.getMediaViewHolderFieldAs<ImageButton>("action3", isDynamicIsland) ?: return null
        val action4 = mMediaViewHolder.getMediaViewHolderFieldAs<ImageButton>("action4", isDynamicIsland) ?: return null
        val seekBar = mMediaViewHolder.getMediaViewHolderFieldAs<SeekBar>("seekBar", isDynamicIsland) ?: return null
        val realSeekBar = mMediaViewHolder.getAdditionalInstanceFieldAs<SeekBar?>(KEY_REAL_PROGRESS_BAR)
        val elapsedTimeView = mMediaViewHolder.getMediaViewHolderFieldAs<TextView>("elapsedTimeView", isDynamicIsland) ?: return null
        val totalTimeView = mMediaViewHolder.getMediaViewHolderFieldAs<TextView>("totalTimeView", isDynamicIsland) ?: return null
        val albumView = mMediaViewHolder.getMediaViewHolderFieldAs<ImageView>("albumImageView", isDynamicIsland) ?: return null
        val mediaBg: ImageView?

        if (isDynamicIsland) {
            val mediaBgView = mMediaViewHolder.getMediaViewHolderFieldAs<View>("mediaBgView", true)
            mediaBg = ImageView(titleText.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                id = mediaBgId
                layoutParams = ViewGroup.LayoutParams(0, 0)
            }.also {
                val parent = titleText.parent as? ViewGroup ?: return@also
                val index: Int
                if (mediaBgView != null) {
                    index = (parent.indexOfChild(mediaBgView) + 1).coerceIn(0, parent.childCount)
                    it.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(p0: View?, p1: Outline?) {
                            if (p0 == null || p1 == null) return
                            val cornerRadius = p0.context?.resources?.getDimension(mediaBgRadiusDi) ?: return
                            p1.setRoundRect(0, 0, p0.width, p0.height, cornerRadius)
                        }
                    }
                    it.clipToOutline = true
                } else {
                    index = 0
                }
                parent.addView(it, index)
                parent.removeView(mediaBgView)
                val constraintSet = EzxHelpUtils.newInstance(clzConstraintSetClass!!)
                clone.invoke(constraintSet, parent)
                connect.invoke(constraintSet, mediaBgId, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
                connect.invoke(constraintSet, mediaBgId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect.invoke(constraintSet, mediaBgId, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, mediaBgId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                setVisibility.invoke(constraintSet, mediaBgViewId, View.GONE)
                applyTo.invoke(constraintSet, parent)
            }
        } else {
            mediaBg = mMediaViewHolder.getMediaViewHolderFieldAs<ImageView>("mediaBg", false) ?: return null
        }

        return MiuiMediaViewHolderWrapper(
            mMediaViewHolder.hashCode(),
            titleText, artistText, albumView, mediaBg, seamlessIcon,
            action0, action1, action2, action3, action4,
            elapsedTimeView, totalTimeView, realSeekBar ?: seekBar
        ).also {
            mMediaViewHolder.setAdditionalInstanceField(KEY_VIEW_HOLDER_WRAPPER, it)
        }
    }

    // ==================== 状态管理 ====================

    private fun getPlayerConfig(type: PlayerType): PlayerConfig = when (type) {
        PlayerType.NOTIFICATION_CENTER -> ncPlayerConfig
        PlayerType.DYNAMIC_ISLAND -> diPlayerConfig
        PlayerType.DUMMY_DYNAMIC_ISLAND -> diPlayerConfigDummy
    }

    private fun finiPlayerConfig(type: PlayerType) {
        when (type) {
            PlayerType.DYNAMIC_ISLAND -> {
                diPlayerConfig.reset()
                diPlayerConfigDummy.reset()
            }
            else -> getPlayerConfig(type).reset()
        }
        releaseCachedWallpaperColor()
    }

    // ==================== 前景色 ====================

    private fun updateForegroundColors(holder: MiuiMediaViewHolderWrapper, colorConfig: MediaViewColorConfig) {
        val primaryColorStateList = ColorStateList.valueOf(colorConfig.textPrimary)
        holder.titleText.setTextColor(colorConfig.textPrimary)
        holder.artistText.setTextColor(colorConfig.textSecondary)
        holder.seamlessIcon.imageTintList = primaryColorStateList
        holder.action0.imageTintList = primaryColorStateList
        holder.action1.imageTintList = primaryColorStateList
        holder.action2.imageTintList = primaryColorStateList
        holder.action3.imageTintList = primaryColorStateList
        holder.action4.imageTintList = primaryColorStateList
        holder.elapsedTimeView.setTextColor(colorConfig.textPrimary)
        holder.totalTimeView.setTextColor(colorConfig.textPrimary)

        val seekBar = holder.seekBar
        if (hyperProgressSeekBar != null && hyperProgressSeekBar!!.isInstance(seekBar)) {
            seekBar.setAdditionalInstanceField(KEY_SEEKBAR_TINT_COLOR, colorConfig.textPrimary)
            seekBar.invalidate()
        } else {
            seekBar.thumbTintList = primaryColorStateList
            seekBar.progressTintList = primaryColorStateList
            seekBar.progressBackgroundTintList = primaryColorStateList
        }
    }


    // ==================== 核心更新 ====================

    @Suppress("UNCHECKED_CAST")
    private fun updateBackground(
        context: Context,
        artwork: Icon?,
        pkgName: String,
        holder: MiuiMediaViewHolderWrapper,
        type: PlayerType,
        processor: BgProcessor?
    ) {
        val artworkLayer = artwork?.loadDrawable(context) ?: return
        val playerConfig = getPlayerConfig(type)
        val reqId = playerConfig.artworkNextBindRequestId++

        holder.albumView.setImageDrawable(artworkLayer)

        val width: Int
        val height: Int
        if (holder.mediaBg.measuredWidth == 0 || holder.mediaBg.measuredHeight == 0) {
            if (playerConfig.lastWidth == 0 || playerConfig.lastHeight == 0) {
                width = artworkLayer.intrinsicWidth
                height = artworkLayer.intrinsicHeight
            } else {
                width = playerConfig.lastWidth
                height = playerConfig.lastHeight
            }
        } else {
            width = holder.mediaBg.measuredWidth
            height = holder.mediaBg.measuredHeight
            playerConfig.lastWidth = width
            playerConfig.lastHeight = height
        }

        updateForegroundColors(holder, playerConfig.currColorConfig)

        HostExecutor.execute(
            tag = type,
            backgroundTask = {
                val mutableColorScheme: Any?
                val artworkDrawable: Drawable

                val wallpaperColors = context.getCachedWallpaperColor(artwork)
                if (wallpaperColors != null) {
                    mutableColorScheme = newColorScheme(wallpaperColors)
                    artworkDrawable = context.getScaledBackground(artwork, height, height)
                        ?: Color.TRANSPARENT.toDrawable()
                } else {
                    artworkDrawable = Color.TRANSPARENT.toDrawable()
                    try {
                        val icon = context.packageManager.getApplicationIcon(pkgName)
                        mutableColorScheme = newColorScheme(WallpaperColors.fromDrawable(icon))
                            ?: throw Exception()
                    } catch (_: Exception) {
                        XposedLog.w(TAG, lpparam.packageName, "updateBackground: app not found")
                        return@execute null
                    }
                }

                var colorConfig = defaultColorConfig
                if (mutableColorScheme != null) {
                    val neutral1 = fldTonalPaletteAllShades?.get(fldColorSchemeNeutral1!!.get(mutableColorScheme)) as? List<Int>
                    val neutral2 = fldTonalPaletteAllShades?.get(fldColorSchemeNeutral2!!.get(mutableColorScheme)) as? List<Int>
                    val accent1 = fldTonalPaletteAllShades?.get(fldColorSchemeAccent1!!.get(mutableColorScheme)) as? List<Int>
                    val accent2 = fldTonalPaletteAllShades?.get(fldColorSchemeAccent2!!.get(mutableColorScheme)) as? List<Int>
                    if (neutral1 != null && neutral2 != null && accent1 != null && accent2 != null) {
                        colorConfig = processor!!.convertToColorConfig(artworkDrawable, neutral1, neutral2, accent1, accent2)
                    }
                }

                val processedArtwork = processor!!.processAlbumCover(artworkDrawable, colorConfig, context, width, height)
                return@execute Pair(colorConfig, processedArtwork)
            },
            runOnMain = true
        ) { pair ->
            if (reqId < playerConfig.artworkBoundId) return@execute

            val colorConfig = pair.first
            val processedArtwork = pair.second

            if (playerConfig.artworkDrawable == null) {
                playerConfig.artworkDrawable = processor!!.createBackground(processedArtwork, colorConfig).apply {
                    setResizeAnim(type == PlayerType.NOTIFICATION_CENTER)
                }
            }
            playerConfig.artworkDrawable?.setBounds(0, 0, width, height)
            playerConfig.currentPkgName = pkgName
            playerConfig.artworkBoundId = reqId

            if (colorConfig != playerConfig.currColorConfig) {
                updateForegroundColors(holder, colorConfig)
                playerConfig.currColorConfig = colorConfig
            }

            holder.mediaBg.setPadding(0, 0, 0, 0)
            holder.mediaBg.setImageDrawable(playerConfig.artworkDrawable)

            val skipAnim = !holder.mediaBg.isShown
            playerConfig.artworkDrawable?.updateAlbumCover(processedArtwork, colorConfig, skipAnim)
            playerConfig.isArtworkBound = true
            XposedLog.d(TAG, lpparam.packageName, "updateBackground: applied pkg=$pkgName type=$type size=${width}x${height}")
        }
    }
}
