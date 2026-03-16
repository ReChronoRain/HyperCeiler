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
 * along with it program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackground.isIsland
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.clzConstraintSetClass
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewBinderImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewHolder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaNotificationControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.playerIslandConstraintLayout
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.clear
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.connect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.setGoneMargin
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.setMargin
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.setVisibility
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object MediaViewLayout : BaseHook() {

    // ==================== 资源 ID ====================

    private val headerTitle by lazy {
        appContext.getIdByName("header_title")
    }
    private val headerArtist by lazy {
        appContext.getIdByName("header_artist")
    }
    private val icon by lazy {
        appContext.getIdByName("icon")
    }
    private val albumArt by lazy {
        appContext.getIdByName("album_art")
    }
    private val mediaSeamless by lazy {
        appContext.getIdByName("media_seamless")
    }
    private val actions by lazy {
        appContext.getIdByName("actions")
    }
    private val action0 by lazy {
        appContext.getIdByName("action0")
    }
    private val action1 by lazy {
        appContext.getIdByName("action1")
    }
    private val action2 by lazy {
        appContext.getIdByName("action2")
    }
    private val action3 by lazy {
        appContext.getIdByName("action3")
    }
    private val action4 by lazy {
        appContext.getIdByName("action4")
    }

    // ==================== 通知中心配置 ====================

    private val ncActionsOrder by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_media_button_mode", 0)
    }
    private val ncAlbum by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0)
    }
    private val ncHeaderMargin by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_title_margin", 210).toFloat() / 10
    }
    private val ncHeaderPadding by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_title_padding", 40).toFloat() / 10
    }
    private val ncHideTime by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_hide_time")
    }
    private val ncHideSeamless by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_hide_seamless")
    }
    private val ncActionsLeftAligned by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_actions_left_aligned")
    }
    private val ncButtonSize by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_media_button", 140)
    }
    private val ncButtonSizeCustom by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_media_button_custom", 140)
    }
    private val ncOnLayout by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_layout_switch")
    }

    // ==================== 灵动岛配置 ====================

    private val diActionsOrder by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_media_button_mode", 0)
    }
    private val diAlbum by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_media_album_mode", 0)
    }
    private val diHeaderMargin by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_title_margin", 210).toFloat() / 10
    }
    private val diHeaderPadding by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_title_padding", 40).toFloat() / 10
    }
    private val diHideTime by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_media_button_hide_time")
    }
    private val diHideSeamless by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_media_button_hide_seamless")
    }
    private val diActionsLeftAligned by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_media_button_actions_left_aligned")
    }
    private val diOnLayout by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_media_button_layout_switch")
    }

    // ==================== 初始化 ====================

    override fun init() {
        if (ncOnLayout) {
            initLayoutConstraints()
            initHideSeamlessHook()
            initButtonSize()
        }

        if (isIsland && diOnLayout) {
            initDynamicIslandLayout()
            initDynamicIslandHideSeamless()
        }
    }

    // ==================== 通知中心布局 ====================

    private fun initLayoutConstraints() {
        val needUpdate = ncAlbum == 2 || ncHeaderMargin != 21.0f || ncHeaderPadding != 4.0f
            || ncActionsLeftAligned || ncHideSeamless || ncHideTime || ncActionsOrder != 0
        if (!needUpdate) return

        val controllerClass = miuiMediaNotificationControllerImpl ?: return
        val loadLayoutMethod = controllerClass.declaredMethods.firstOrNull { it.name.startsWith("loadLayout") }

        if (loadLayoutMethod != null) {
            controllerClass.afterHookMethod(loadLayoutMethod.name) {
                val normalLayout = it.thisObject.getObjectFieldOrNull("normalLayout")
                    ?: return@afterHookMethod
                val normalAlbumLayout = it.thisObject.getObjectFieldOrNull("normalAlbumLayout")
                updateConstraintSet(normalLayout, ncAlbum, ncHeaderMargin, ncHeaderPadding, ncActionsOrder, ncActionsLeftAligned, ncHideSeamless)
                if (ncAlbum != 0 && normalAlbumLayout != null) {
                    setVisibility.invoke(normalAlbumLayout, icon, View.GONE)
                }
            }
        } else {
            controllerClass.afterHookConstructor {
                val normalLayout = it.thisObject.getObjectFieldOrNull("normalLayout")
                    ?: return@afterHookConstructor
                updateConstraintSet(normalLayout, ncAlbum, ncHeaderMargin, ncHeaderPadding, ncActionsOrder, ncActionsLeftAligned, ncHideSeamless)
            }
        }
    }

    private fun initHideSeamlessHook() {
        if (!ncHideSeamless) return
        miuiMediaViewControllerImpl?.beforeHookMethod("setSeamless") { it.result = null }
    }

    private fun initButtonSize() {
        if (ncButtonSize == 140 && ncButtonSizeCustom == 140) return

        val drawableUtils = findClassIfExists("com.miui.utils.DrawableUtils") ?: return
        val targetClass = miuiMediaViewControllerImpl ?: return

        targetClass.beforeHookMethod("bindButtonCommon") {
            val mediaAction = it.args[1] ?: return@beforeHookMethod
            val button = it.args[0] as ImageButton
            val desc = mediaAction.getObjectFieldOrNullAs<String>("contentDescription")
                ?: return@beforeHookMethod

            val isMainButton = desc.contains("Play") || desc.contains("Pause")
                || desc.contains("Previous track") || desc.contains("Next track")

            val targetSize = when {
                ncButtonSizeCustom != 140 && !isMainButton -> ncButtonSizeCustom
                ncButtonSize != 140 && isMainButton -> ncButtonSize
                ncButtonSize != 140 && !isMainButton -> ncButtonSize
                else -> return@beforeHookMethod
            }

            val loadDrawable = mediaAction.getObjectFieldOrNullAs<Drawable>("icon")
                ?: return@beforeHookMethod
            val method = drawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable::class.java)
            val bitmap = method.invoke(null, loadDrawable) as Bitmap
            val scaledBitmap = bitmap.scale(targetSize, targetSize)
            mediaAction.setObjectField("icon", scaledBitmap.toDrawable(button.context.resources))
        }
    }

    // ==================== 灵动岛布局 ====================

    private fun initDynamicIslandLayout() {
        val needUpdate = diAlbum == 2 || diActionsLeftAligned || diHideTime || diHideSeamless
            || diHeaderMargin != 21.0f || diHeaderPadding != 4.0f || diActionsOrder != 0
        if (!needUpdate) return

        miuiIslandMediaControllerImpl?.let { controllerClass ->
            val reInflateMethod = controllerClass.declaredMethods
                .firstOrNull { it.name.startsWith("reInflateView") } ?: return@let

            controllerClass.afterHookMethod(reInflateMethod.name) { param ->
                val normalLayoutIsland = param.thisObject.getObjectFieldOrNull("normalLayoutIsland")
                    ?: return@afterHookMethod
                updateConstraintSet(normalLayoutIsland, diAlbum, diHeaderMargin, diHeaderPadding, diActionsOrder, diActionsLeftAligned, diHideSeamless)

                val miuiPlayerHolder = param.thisObject.getObjectFieldOrNull("miuiPlayerHolder")
                val miuiDummyPlayerHolder = param.thisObject.getObjectFieldOrNull("miuiDummyPlayerHolder")

                val fldPlayer = miuiIslandMediaViewHolder?.findField("player")
                    ?.apply { isAccessible = true }
                val applyToMethod = clzConstraintSetClass!!.declaredMethods
                    .first { it.name == "applyTo" }

                miuiPlayerHolder?.let {
                    fldPlayer?.get(it)?.let { player ->
                        applyToMethod.invoke(normalLayoutIsland, player)
                    }
                }
                miuiDummyPlayerHolder?.let {
                    fldPlayer?.get(it)?.let { player ->
                        applyToMethod.invoke(normalLayoutIsland, player)
                    }
                }

                if (diAlbum != 0) {
                    val fldAppIcon = miuiIslandMediaViewHolder?.getDeclaredField("appIcon")
                        ?.apply { isAccessible = true }
                    miuiPlayerHolder?.let { (fldAppIcon?.get(it) as? ImageView)?.visibility = View.GONE }
                    miuiDummyPlayerHolder?.let { (fldAppIcon?.get(it) as? ImageView)?.visibility = View.GONE }
                }
            }
        }

        playerIslandConstraintLayout?.let { clz ->
            clz.constructors.firstOrNull { it.parameterCount == 3 }?.let {
                clz.afterHookConstructor(*it.parameterTypes) { param ->
                    val normalLayoutIsland = param.thisObject.getObjectFieldOrNull("normalLayoutIsland")
                        ?: return@afterHookConstructor
                    updateConstraintSet(normalLayoutIsland, diAlbum, diHeaderMargin, diHeaderPadding, diActionsOrder, diActionsLeftAligned, diHideSeamless)
                }
            }
        }
    }

    private fun initDynamicIslandHideSeamless() {
        if (!diHideSeamless) return
        miuiIslandMediaViewBinderImpl?.beforeHookMethod("setSeamless") { it.result = null }
    }

    // ==================== 公共方法 ====================

    private fun updateConstraintSet(
        constraintSet: Any,
        album: Int,
        headerMargin: Float,
        headerPadding: Float,
        actionsOrder: Int,
        actionsLeftAligned: Boolean,
        hideSeamless: Boolean
    ) {
        val standardMargin = dp2px(26)

        if (album == 2) {
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.START, standardMargin)
            setGoneMargin.invoke(constraintSet, headerArtist, ConstraintSet.START, standardMargin)
            setGoneMargin.invoke(constraintSet, actions, ConstraintSet.TOP, dp2px(67.5f))
            setGoneMargin.invoke(constraintSet, action0, ConstraintSet.TOP, dp2px(78.5f))
            setVisibility.invoke(constraintSet, albumArt, View.GONE)
        }

        if (headerMargin != 21.0f) {
            val headerMarginTop = dp2px(headerMargin)
            setMargin.invoke(constraintSet, headerTitle, ConstraintSet.TOP, headerMarginTop)
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.TOP, headerMarginTop)
        }

        if (headerPadding != 4.0f) {
            setMargin.invoke(constraintSet, headerArtist, ConstraintSet.TOP, dp2px(headerPadding))
        }

        when (actionsOrder) {
            1 -> {
                connect.invoke(constraintSet, action1, ConstraintSet.LEFT, actions, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action1, ConstraintSet.RIGHT, action2, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action2, ConstraintSet.LEFT, action1, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action2, ConstraintSet.RIGHT, action3, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action3, ConstraintSet.LEFT, action2, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action3, ConstraintSet.RIGHT, action0, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action0, ConstraintSet.LEFT, action3, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action0, ConstraintSet.RIGHT, action4, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action4, ConstraintSet.LEFT, action0, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action4, ConstraintSet.RIGHT, actions, ConstraintSet.RIGHT)
                setMargin.invoke(constraintSet, action0, ConstraintSet.START, 0)
                setMargin.invoke(constraintSet, action1, ConstraintSet.START, dp2px(6))
            }
            2 -> {
                connect.invoke(constraintSet, action2, ConstraintSet.LEFT, actions, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action2, ConstraintSet.RIGHT, action1, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action1, ConstraintSet.LEFT, action2, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action1, ConstraintSet.RIGHT, action3, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action3, ConstraintSet.LEFT, action1, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action3, ConstraintSet.RIGHT, action0, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action0, ConstraintSet.LEFT, action3, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action0, ConstraintSet.RIGHT, action4, ConstraintSet.LEFT)
                connect.invoke(constraintSet, action4, ConstraintSet.LEFT, action0, ConstraintSet.RIGHT)
                connect.invoke(constraintSet, action4, ConstraintSet.RIGHT, actions, ConstraintSet.RIGHT)
                setMargin.invoke(constraintSet, action0, ConstraintSet.START, 0)
                setMargin.invoke(constraintSet, action2, ConstraintSet.START, dp2px(6))
            }
        }

        if (actionsLeftAligned) {
            clear.invoke(constraintSet, action4, ConstraintSet.RIGHT)
        }

        if (hideSeamless) {
            setVisibility.invoke(constraintSet, mediaSeamless, View.GONE)
            setGoneMargin.invoke(constraintSet, headerTitle, ConstraintSet.END, standardMargin)
            setGoneMargin.invoke(constraintSet, headerArtist, ConstraintSet.END, standardMargin)
        }
    }
}

