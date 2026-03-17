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
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackground.isIsland
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isDarkMode
import com.sevtinge.hyperceiler.libhook.utils.api.HostExecutor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldColorSchemeAccent1
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.fldTonalPaletteAllShades
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.getCachedWallpaperColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.newColorScheme
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory.releaseCachedWallpaperColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.clzConstraintSetClass
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaData
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewBinderImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable.AmbientLightDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.PlayerType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.applyTo
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.clone
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.connect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.getMediaViewHolderFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object AmbientLight : BaseHook() {
    private const val KEY_MEDIA_BG_VIEW = "KEY_AMBIENT_LIGHT_VIEW"
    private const val KEY_MEDIA_BG_COLOR_LIGHT = "KEY_MEDIA_BG_COLOR_LIGHT"
    private const val KEY_MEDIA_BG_COLOR_DARK = "KEY_MEDIA_BG_COLOR_DARK"

    private val ncBackgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0)
    }
    private val ncAmbientLight by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_ambient_light")
    }
    private val ncAmbientColorOpt by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_ambient_light_opt")
    }
    private val ncAlwaysDark by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_always_dark")
    }

    private val diBackgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_background_mode", 0)
    }
    private val diAmbientLightType by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_ambient_light_type", 0)
    }
    private val diAmbientColorOpt by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_ambient_light_opt")
    }

    var ncCurrentPkgName = ""
    var ncIsArtworkBound = false
    var diCurrentPkgName = ""
    var diIsArtworkBound = false
    var inFullAod = false

    private val mediaBgViewId by lazy {
        appContext.getIdByName("media_bg_view")
    }

    private val fldIsPlaying by lazy { mediaData?.findFieldOrNull("isPlaying") }
    private val fldArtwork by lazy { mediaData?.findFieldOrNull("artwork") }
    private val fldPackageName by lazy { mediaData?.findFieldOrNull("packageName") }

    private val metGetMainColorHCT by lazy {
        findClassIfExists("miuix.mipalette.MiPalette")?.runCatching {
            declaredMethods.firstOrNull {
                it.name == "getMainColorHCT" && it.parameterTypes.contentEquals(arrayOf(Bitmap::class.java))
            }
        }?.getOrNull()
    }
    private val metDrawable2Bitmap by lazy {
        findClassIfExists("com.miui.utils.DrawableUtils")?.runCatching {
            declaredMethods.firstOrNull {
                it.name == "drawable2Bitmap" && it.parameterTypes.contentEquals(arrayOf(Drawable::class.java))
            }
        }?.getOrNull()
    }
    private val metAcquireApplicationIcon by lazy {
        findClassIfExists("com.android.systemui.statusbar.notification.utils.NotificationUtil")
            ?.declaredMethods?.firstOrNull { it.name == "acquireApplicationIcon" }
    }

    override fun init() {
        // 通知中心氛围光
        if (ncBackgroundStyle == 0 && ncAmbientLight) {
            initNotificationCenter()
        }
        // 灵动岛氛围光
        if (isIsland && diBackgroundStyle == 0 && diAmbientLightType != 0) {
            initDynamicIsland()
        }
    }

    // ==================== 通知中心 ====================

    private fun initNotificationCenter() {
        val controllerClass = miuiMediaViewControllerImpl ?: return

        val fldEnableFullAod = findClassIfExists(
            "com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController"
        )?.findFieldOrNull("mEnableFullAod")

        val metLazyGet = findClassIfExists("dagger.Lazy")
            ?.declaredMethods?.firstOrNull { it.name == "get" }

        fun Any.isEnableFullAod(): Boolean {
            val controller = getObjectFieldOrNull("fullAodController")
                ?.let { metLazyGet?.invoke(it) } ?: return false
            return fldEnableFullAod?.get(controller) == true
        }

        fun Any.isDark(): Boolean = isEnableFullAod() || isDarkMode() || (ncBackgroundStyle == 0 && ncAlwaysDark)

        controllerClass.apply {
            afterHookMethod("detach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                (getAmbientLightView(holder)?.drawable as? AmbientLightDrawable)?.stop()
                ncCurrentPkgName = ""
                ncIsArtworkBound = false
                releaseCachedWallpaperColor()
            }

            afterHookMethod("attach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                getOrCreateAmbientLightView(holder, false)
            }

            afterHookMethod("bindMediaData") { param ->
                val mediaData = param.args[0] ?: return@afterHookMethod
                val packageName = fldPackageName?.get(mediaData) as? String ?: return@afterHookMethod
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val context = param.thisObject.getObjectFieldOrNullAs<Context>("context") ?: return@afterHookMethod

                val isArtWorkUpdate = param.thisObject.getObjectFieldOrNull("isArtWorkUpdate") == true
                    || ncCurrentPkgName != packageName || !ncIsArtworkBound

                if (isArtWorkUpdate) {
                    val isDark = param.thisObject.isDark()
                    updateColor(context, mediaData, packageName, holder, PlayerType.NOTIFICATION_CENTER, isDark)
                    XposedLog.d(TAG, lpparam.packageName, "bindMediaData: artworkUpdate pkg=$packageName isDark=$isDark")
                } else {
                    val mediaBgView = getAmbientLightView(holder) ?: return@afterHookMethod
                    val drawable = mediaBgView.drawable as? AmbientLightDrawable ?: return@afterHookMethod
                    val isPlaying = fldIsPlaying?.get(mediaData) == true
                    if (!inFullAod && isPlaying) drawable.resume() else drawable.pause()
                }
            }

            afterHookMethod("onFullAodStateChanged") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val drawable = getAmbientLightView(holder)?.drawable as? AmbientLightDrawable ?: return@afterHookMethod
                val mediaData = param.thisObject.getObjectFieldOrNull("mediaData") ?: return@afterHookMethod
                val toFullAod = param.args[0] as Boolean
                if (toFullAod != inFullAod) drawable.animateNextResize()
                inFullAod = toFullAod
                val isPlaying = fldIsPlaying?.get(mediaData) == true
                if (!toFullAod && isPlaying) drawable.resume() else drawable.pause()
            }

            beforeHookMethod("updateForegroundColors") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@beforeHookMethod
                val mediaBgView = getAmbientLightView(holder) ?: return@beforeHookMethod
                val drawable = mediaBgView.drawable as? AmbientLightDrawable ?: return@beforeHookMethod
                val isDark = param.thisObject.isDark()

                if (ncAmbientColorOpt) {
                    val light = holder.getAdditionalInstanceFieldAs(KEY_MEDIA_BG_COLOR_LIGHT) ?: Color.TRANSPARENT
                    val dark = holder.getAdditionalInstanceFieldAs(KEY_MEDIA_BG_COLOR_DARK) ?: Color.TRANSPARENT
                    drawable.setGradientColor(if (isDark) dark else light, !mediaBgView.isShown)
                }
                drawable.setLightMode(!isDark)
                XposedLog.d(TAG, lpparam.packageName, "updateForegroundColors: isDark=$isDark, colorOpt=$ncAmbientColorOpt")
            }
        }
    }

    // ==================== 灵动岛 ====================

    private fun initDynamicIsland() {
        val binderClass = miuiIslandMediaViewBinderImpl ?: return
        val fldMediaBgTransYOffset = runCatching { binderClass.findField("mediaBgTransYOffset") }.getOrNull()

        binderClass.apply {
            if (diAmbientLightType == 1) {
                // 仅移除背景
                afterHookMethod("attach") { param ->
                    removeDiMediaBg(param.thisObject.getObjectFieldOrNull("holder"))
                    removeDiMediaBg(param.thisObject.getObjectFieldOrNull("dummyHolder"))
                }
            } else {
                // 完整氛围光
                afterHookMethod("detach") { param ->
                    val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                    val dummyHolder = param.thisObject.getObjectFieldOrNull("dummyHolder") ?: return@afterHookMethod
                    (getAmbientLightView(holder)?.drawable as? AmbientLightDrawable)?.stop()
                    (getAmbientLightView(dummyHolder)?.drawable as? AmbientLightDrawable)?.stop()
                    diCurrentPkgName = ""
                    diIsArtworkBound = false
                    releaseCachedWallpaperColor()
                }

                afterHookMethod("attach") { param ->
                    param.thisObject.getObjectFieldOrNull("holder")?.let { getOrCreateAmbientLightView(it, true) }
                    param.thisObject.getObjectFieldOrNull("dummyHolder")?.let { getOrCreateAmbientLightView(it, true) }
                }

                afterHookMethod("bindMediaData") { param ->
                    val mediaData = param.args[0] ?: return@afterHookMethod
                    val packageName = fldPackageName?.get(mediaData) as? String ?: return@afterHookMethod
                    val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                    val dummyHolder = param.thisObject.getObjectFieldOrNull("dummyHolder") ?: return@afterHookMethod
                    val context = param.thisObject.getObjectFieldOrNullAs<Context>("context") ?: return@afterHookMethod

                    val isArtWorkUpdate = param.thisObject.getObjectFieldOrNull("isArtWorkUpdate") == true
                        || diCurrentPkgName != packageName || !diIsArtworkBound

                    if (isArtWorkUpdate) {
                        updateColor(context, mediaData, packageName, holder, PlayerType.DYNAMIC_ISLAND, true)
                        updateColor(context, mediaData, packageName, dummyHolder, PlayerType.DUMMY_DYNAMIC_ISLAND, true)
                    } else {
                        val isPlaying = fldIsPlaying?.get(mediaData) == true
                        resumeOrPauseDi(holder, isPlaying)
                        resumeOrPauseDi(dummyHolder, isPlaying)
                    }

                    val mediaBgTransYOffset = fldMediaBgTransYOffset?.get(param.thisObject) as? Float
                    if (mediaBgTransYOffset != null && mediaBgTransYOffset != 0.0f) {
                        getAmbientLightView(dummyHolder)?.translationY = mediaBgTransYOffset
                    }
                }
            }
        }

        if (diAmbientLightType != 1) {
            findClassIfExists(
                "com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewBinderImpl\$attach\$4\$1"
            )?.apply {
                val fldEmitHolder = findFieldOrNull($$"$holder")
                val fldEmitDummyHolder = findFieldOrNull($$"$dummyHolder")

                afterHookMethod("emit") { param ->
                    val pair = param.args[0] as? Pair<*, *> ?: return@afterHookMethod
                    val action = pair.first as? String ?: return@afterHookMethod
                    val data = pair.second as? Bundle
                    val mediaBgView = fldEmitHolder?.get(param.thisObject)?.let { getAmbientLightView(it) } ?: return@afterHookMethod
                    val dummyMediaBgView = fldEmitDummyHolder?.get(param.thisObject)?.let { getAmbientLightView(it) } ?: return@afterHookMethod

                    when (action) {
                        "pull_down_type_start" -> {
                            (mediaBgView.drawable as? AmbientLightDrawable)?.pause()
                        }
                        "pull_down_type_update" -> {
                            dummyMediaBgView.translationY = data?.getFloat("pull_down_action_offset_y", 0.0f) ?: 0.0f
                        }
                        "pull_down_type_finish" -> {
                            (mediaBgView.drawable as? AmbientLightDrawable)?.resume()
                        }
                    }
                }
            }
        }
    }

    private fun removeDiMediaBg(holder: Any?) {
        holder ?: return
        val mediaBgView = holder.getMediaViewHolderFieldAs<View>("mediaBgView", true) ?: return
        (mediaBgView.parent as? ViewGroup)?.removeView(mediaBgView)
    }

    private fun resumeOrPauseDi(holder: Any, isPlaying: Boolean) {
        val drawable = getAmbientLightView(holder)?.drawable as? AmbientLightDrawable ?: return
        if (isPlaying) drawable.resume() else drawable.pause()
    }

    // ==================== 公共方法 ====================

    private fun getAmbientLightView(holder: Any): ImageView? {
        val view = holder.getAdditionalInstanceFieldAs<ImageView>(KEY_MEDIA_BG_VIEW)
        return view?.takeIf { it.drawable is AmbientLightDrawable }
    }

    private fun getOrCreateAmbientLightView(holder: Any, isDynamicIsland: Boolean): ImageView? {
        getAmbientLightView(holder)?.let { return it }

        val mediaBg = if (isDynamicIsland) {
            holder.getMediaViewHolderFieldAs<View>("mediaBgView", true)
        } else {
            holder.getMediaViewHolderFieldAs<View>("mediaBg", false)
        } ?: return null
        val parent = mediaBg.parent as? ViewGroup ?: return null
        val index = (parent.indexOfChild(mediaBg) + 1).coerceIn(0, parent.childCount)

        val drawable = AmbientLightDrawable().apply { start() }
        val view = ImageView(mediaBg.context).apply {
            id = mediaBgViewId
            layoutParams = ViewGroup.LayoutParams(0, 0)
            clipToOutline = true
            outlineProvider = mediaBg.outlineProvider
            setImageDrawable(drawable)
        }
        parent.addView(view, index)

        // 灵动岛移除原背景
        if (isDynamicIsland) {
            parent.removeView(mediaBg)
        }

        runCatching {
            val cs = EzxHelpUtils.newInstance(clzConstraintSetClass!!)
            clone.invoke(cs, parent)
            connect.invoke(cs, mediaBgViewId, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
            connect.invoke(cs, mediaBgViewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect.invoke(cs, mediaBgViewId, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
            connect.invoke(cs, mediaBgViewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            applyTo.invoke(cs, parent)
        }.onFailure {
            XposedLog.w(TAG, lpparam.packageName, "ConstraintSet apply failed: ${it.message}")
        }

        holder.setAdditionalInstanceField(KEY_MEDIA_BG_VIEW, view)
        return view
    }

    private fun getMainColorHCT(drawable: Drawable): Int? {
        val bitmap = metDrawable2Bitmap?.invoke(null, drawable) ?: return null
        return metGetMainColorHCT?.invoke(null, bitmap) as? Int
    }

    fun updateColor(
        context: Context,
        mediaData: Any,
        pkgName: String,
        holder: Any,
        type: PlayerType,
        isDark: Boolean
    ) {
        val isDynamicIsland = (type != PlayerType.NOTIFICATION_CENTER)
        val mediaBgView = getOrCreateAmbientLightView(holder, isDynamicIsland) ?: return
        val drawable = mediaBgView.drawable as? AmbientLightDrawable ?: return
        val artwork = fldArtwork?.get(mediaData) as? Icon
        val colorOpt = if (isDynamicIsland) diAmbientColorOpt else ncAmbientColorOpt

        HostExecutor.execute(
            tag = type,
            backgroundTask = {
                val mainColorHCT: Int
                if (colorOpt) {
                    val wallpaperColors = context.getCachedWallpaperColor(artwork)
                    val colorScheme = if (wallpaperColors != null) {
                        newColorScheme(wallpaperColors)
                    } else {
                        try {
                            val icon = context.packageManager.getApplicationIcon(pkgName)
                            newColorScheme(WallpaperColors.fromDrawable(icon)) ?: throw Exception()
                        } catch (_: Exception) {
                            return@execute null
                        }
                    }
                    val accent1 = (fldTonalPaletteAllShades?.get(
                        fldColorSchemeAccent1!!.get(colorScheme)
                    ) as? List<*>)?.filterIsInstance<Int>()

                    if (accent1?.size != 13) {
                        mainColorHCT = Color.TRANSPARENT
                    } else if (isDynamicIsland) {
                        mainColorHCT = accent1[7]
                    } else {
                        val light = accent1[4]
                        val dark = accent1[7]
                        holder.setAdditionalInstanceField(KEY_MEDIA_BG_COLOR_LIGHT, light)
                        holder.setAdditionalInstanceField(KEY_MEDIA_BG_COLOR_DARK, dark)
                        mainColorHCT = if (isDark) dark else light
                    }
                } else {
                    val artDrawable = artwork?.loadDrawable(context)
                        ?: (metAcquireApplicationIcon?.invoke(null, context, mediaData) as? Drawable)
                        ?: return@execute null
                    mainColorHCT = getMainColorHCT(artDrawable) ?: Color.TRANSPARENT
                }
                return@execute mainColorHCT
            },
            runOnMain = true
        ) { mainColorHCT ->
            val isPlaying = fldIsPlaying?.get(mediaData) == true
            drawable.setGradientColor(mainColorHCT, !mediaBgView.isShown)
            if (isPlaying && !(type == PlayerType.NOTIFICATION_CENTER && inFullAod)) {
                drawable.resume()
            } else {
                drawable.pause()
            }
            if (isDynamicIsland) {
                diCurrentPkgName = pkgName
                diIsArtworkBound = true
            } else {
                ncCurrentPkgName = pkgName
                ncIsArtworkBound = true
            }
        }
    }
}
