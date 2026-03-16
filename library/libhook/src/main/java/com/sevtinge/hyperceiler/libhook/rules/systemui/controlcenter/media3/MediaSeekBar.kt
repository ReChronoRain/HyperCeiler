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

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.updateMargins
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackground.isIsland
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isDarkMode
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.api.dp
import com.sevtinge.hyperceiler.libhook.utils.api.dpFloat
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.hyperProgressSeekBar
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.mediaViewHolderNew
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewBinderImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiIslandMediaViewHolder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.seekBarObserver
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.PublicClass.seekBarObserverNew
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.getMediaViewHolderFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.progress.CometSeekBar
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.progress.SquigglySeekBar
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.progress.ThumbStyle
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.appContext

object MediaSeekBar : BaseHook() {
    private const val KEY_REAL_SEEKBAR = "KEY_REAL_PROGRESS_BAR"

    // ==================== 通知中心配置 ====================
    private val ncOn by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_progress_on")
    }
    private val ncProgressMode by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0)
    }
    private val ncThumbMode by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_progress_thumb_mode", 0)
    }
    private val ncProgressThickness by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_progress_thickness", 80)
    }
    private val ncCornerRadius by lazy {
        PrefsBridge.getInt("system_ui_control_center_media_control_progress_corner_radius", 36)
    }
    private val ncProgressComet by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_progress_comet")
    }
    private val ncProgressRound by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_progress_round")
    }

    private val ncCustomThumbStyle by lazy {
        when (ncThumbMode) {
            1 -> ThumbStyle.VerticalBar
            2 -> if (ncProgressRound) ThumbStyle.RoundRect else ThumbStyle.Hidden
            else -> ThumbStyle.Circle
        }
    }

    // ==================== 灵动岛配置 ====================
    private val diOn by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_progress_on")
    }
    private val diProgressMode by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_progress_mode", 0)
    }
    private val diThumbMode by lazy {
        PrefsBridge.getStringAsInt("system_ui_island_media_control_progress_thumb_mode", 0)
    }
    private val diProgressThickness by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_progress_thickness", 6)
    }
    private val diCornerRadius by lazy {
        PrefsBridge.getInt("system_ui_island_media_control_progress_corner_radius", 36)
    }
    private val diProgressComet by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_progress_comet")
    }
    private val diProgressRound by lazy {
        PrefsBridge.getBoolean("system_ui_island_media_control_progress_round")
    }

    private val diCustomThumbStyle by lazy {
        when (diThumbMode) {
            1 -> ThumbStyle.VerticalBar
            2 -> if (diProgressRound) ThumbStyle.RoundRect else ThumbStyle.Hidden
            else -> ThumbStyle.Circle
        }
    }

    // ==================== 公共字段 ====================
    private val ncBackgroundStyle by lazy {
        PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0)
    }
    private val ncAlwaysDark by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_media_control_always_dark")
    }

    private val clzProgress by lazy {
        findClassIfExists("com.android.systemui.media.controls.ui.viewmodel.SeekBarViewModel\$Progress")
    }
    private val clzSeekBarViewModel by lazy {
        findClassIfExists("com.android.systemui.media.controls.ui.viewmodel.SeekBarViewModel")
    }

    private val fldProgressSeekBarMinHeight by lazy {
        hyperProgressSeekBar?.findFieldOrNull("mProgressSeekBarMinHeight")
    }
    private val fldProgressHeight by lazy {
        hyperProgressSeekBar?.findFieldOrNull("mProgressHeight")
    }
    private val fldListening by lazy { clzProgress?.findFieldOrNull("listening") }
    private val fldSeekAvailable by lazy { clzProgress?.findFieldOrNull("seekAvailable") }
    private val fldPlaying by lazy { clzProgress?.findFieldOrNull("playing") }
    private val fldScrubbing by lazy { clzProgress?.findFieldOrNull("scrubbing") }
    private val fldEnabled by lazy { clzProgress?.findFieldOrNull("enabled") }
    private val fldDuration by lazy { clzProgress?.findFieldOrNull("duration") }
    private val fldElapsedTime by lazy { clzProgress?.findFieldOrNull("elapsedTime") }

    private val fldFalsingManager by lazy {
        clzSeekBarViewModel?.findFieldOrNull("falsingManager")
    }
    private val ctorSeekBarChangeListener by lazy {
        findClassIfExists($$"com.android.systemui.media.controls.ui.viewmodel.SeekBarViewModel$SeekBarChangeListener")
            ?.constructors?.firstOrNull { it.parameterCount == 2 }
    }

    private val mediaBgViewId by lazy {
        appContext.getIdByName("media_progress_bar")
    }

    override fun init() {
        if (ncOn) {
            initNotificationCenter()
        }
        if (isIsland && diOn) {
            initDynamicIsland()
        }
    }

    // ==================== 通知中心 ====================

    private fun initNotificationCenter() {
        if (ncProgressMode == 0) {
            initNcDefaultProgressWidth()
            return
        }
        initNcCustomSeekBar()
    }

    private fun initNcDefaultProgressWidth() {
        if (hyperProgressSeekBar == null) return
        mediaViewHolderNew?.afterHookConstructor {
            val seekBar = it.thisObject.getObjectFieldOrNullAs<SeekBar>("seekBar") ?: return@afterHookConstructor
            if (hyperProgressSeekBar?.isInstance(seekBar) != true) return@afterHookConstructor
            applyProgressHeight(seekBar, ncProgressThickness)
        }
    }

    private fun initNcCustomSeekBar() {
        mediaViewHolderNew?.afterHookConstructor {
            getOrCreateRealSeekBar(it.thisObject, false)
        }

        if (isMoreAndroidVersion(36)) {
            seekBarObserverNew?.beforeHookMethod("onChanged") {
                val controllerImpl =
                    it.thisObject.getObjectFieldOrNull($$"this$0") ?: return@beforeHookMethod
                val holder =
                    controllerImpl.getObjectFieldOrNull("holder") ?: return@beforeHookMethod
                val vmProgress = it.args[0] ?: return@beforeHookMethod
                onProgressChanged(holder, vmProgress, false)
                it.result = null
            }
        } else {
            seekBarObserver?.beforeHookMethod("onChanged") {
                val holder =
                    it.thisObject.getObjectFieldOrNull("holder") ?: return@beforeHookMethod
                val vmProgress = it.args[0] ?: return@beforeHookMethod
                onProgressChanged(holder, vmProgress, false)
                it.result = null
            }
        }

        val controllerClass = miuiMediaViewControllerImpl ?: return

        val fldEnableFullAod = findClassIfExists(
            "com.android.systemui.statusbar.notification.fullaod.NotifiFullAodController"
        )?.findFieldOrNull("mEnableFullAod")
        val metLazyGet = findClassIfExists("dagger.Lazy")
            ?.declaredMethods?.firstOrNull { it.name == "get" }

        controllerClass.apply {
            afterHookMethod("detach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                getOrCreateRealSeekBar(holder, false)?.setOnSeekBarChangeListener(null)
            }

            afterHookMethod("attach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val seekBarViewModel = param.thisObject.getObjectFieldOrNull("seekBarViewModel") ?: return@afterHookMethod
                bindSeekBarListener(holder, seekBarViewModel, false)
            }

            afterHookMethod("onFullAodStateChanged") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val toFullAod = param.args[0] as Boolean
                getOrCreateRealSeekBar(holder, false)?.visibility = if (toFullAod) View.GONE else View.VISIBLE
            }

            afterHookMethod("updateForegroundColors") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val controller = param.thisObject.getObjectFieldOrNull("fullAodController")
                    ?.let { metLazyGet?.invoke(it) }
                val enableFullAod = fldEnableFullAod?.get(controller) == true
                val isDark = enableFullAod || isDarkMode() || (ncBackgroundStyle == 0 && ncAlwaysDark)
                val seekBar = getOrCreateRealSeekBar(holder, false) ?: return@afterHookMethod
                seekBar.progressTintList = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)
                XposedLog.d(TAG, lpparam.packageName, "updateForegroundColors: isDark=$isDark (fullAod=$enableFullAod, alwaysDark=$ncAlwaysDark)")
            }
        }
    }

    // ==================== 灵动岛 ====================

    private fun initDynamicIsland() {
        if (diProgressMode == 0) {
            initDiDefaultProgressWidth()
            return
        }
        initDiCustomSeekBar()
    }

    private fun initDiDefaultProgressWidth() {
        if (hyperProgressSeekBar == null) return
        miuiIslandMediaViewHolder?.afterHookConstructor {
            val seekBar = it.thisObject.getMediaViewHolderFieldAs<SeekBar>("seekBar", true) ?: return@afterHookConstructor
            if (hyperProgressSeekBar?.isInstance(seekBar) != true) return@afterHookConstructor
            applyProgressHeight(seekBar, diProgressThickness)
        }
    }

    private fun initDiCustomSeekBar() {
        miuiIslandMediaViewHolder?.afterHookConstructor {
            getOrCreateRealSeekBar(it.thisObject, true)
        }

        miuiIslandMediaViewBinderImpl?.let { binderClass ->
            binderClass.declaredMethods.firstOrNull {
                it.name.contains("seekBarChanged") && java.lang.reflect.Modifier.isStatic(it.modifiers) && it.parameterCount == 3
            }?.let { method ->
                binderClass.beforeHookMethod(method.name, *method.parameterTypes) { param ->
                    val holder = param.args[2] ?: return@beforeHookMethod
                    val vmProgress = param.args[1] ?: return@beforeHookMethod
                    onProgressChanged(holder, vmProgress, true)
                    param.result = null
                }
            }

            binderClass.afterHookMethod("detach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val dummyHolder = param.thisObject.getObjectFieldOrNull("dummyHolder") ?: return@afterHookMethod
                getOrCreateRealSeekBar(holder, true)?.setOnSeekBarChangeListener(null)
                getOrCreateRealSeekBar(dummyHolder, true)?.setOnSeekBarChangeListener(null)
            }

            binderClass.afterHookMethod("attach") { param ->
                val holder = param.thisObject.getObjectFieldOrNull("holder") ?: return@afterHookMethod
                val dummyHolder = param.thisObject.getObjectFieldOrNull("dummyHolder") ?: return@afterHookMethod
                val seekBarViewModel = param.thisObject.getObjectFieldOrNull("seekBarViewModel") ?: return@afterHookMethod
                bindSeekBarListener(holder, seekBarViewModel, true)
                bindSeekBarListener(dummyHolder, seekBarViewModel, true)
            }
        }
    }

    // ==================== 公共方法 ====================

    private fun applyProgressHeight(seekBar: SeekBar, thickness: Int) {
        val context = seekBar.context
        var height = dp2px(context, thickness.toFloat())
        if (height % 2 != 0) height -= 1
        fldProgressSeekBarMinHeight?.apply { isAccessible = true }?.set(seekBar, height)
        fldProgressHeight?.apply { isAccessible = true }?.set(seekBar, height)
    }

    private fun bindSeekBarListener(holder: Any, seekBarViewModel: Any, isDynamicIsland: Boolean) {
        val falsingManager = fldFalsingManager?.get(seekBarViewModel)
        val listener = ctorSeekBarChangeListener?.newInstance(seekBarViewModel, falsingManager) as? SeekBar.OnSeekBarChangeListener
        getOrCreateRealSeekBar(holder, isDynamicIsland)?.setOnSeekBarChangeListener(listener)
    }

    @SuppressLint("SetTextI18n")
    private fun onProgressChanged(holder: Any, vmProgress: Any, isDynamicIsland: Boolean) {
        val seekBar = getOrCreateRealSeekBar(holder, isDynamicIsland) ?: return
        val elapsedTimeView = holder.getMediaViewHolderFieldAs<TextView>("elapsedTimeView", isDynamicIsland)
        val totalTimeView = holder.getMediaViewHolderFieldAs<TextView>("totalTimeView", isDynamicIsland)

        val listening = fldListening?.get(vmProgress) == true
        val seekAvailable = fldSeekAvailable?.get(vmProgress) == true
        val playing = fldPlaying?.get(vmProgress) == true
        val scrubbing = fldScrubbing?.get(vmProgress) == true
        val enabled = fldEnabled?.get(vmProgress) == true
        val duration = fldDuration?.get(vmProgress) as? Int ?: 0
        val elapsedTime = fldElapsedTime?.get(vmProgress) as? Int

        if (enabled) {
            totalTimeView?.text = DateUtils.formatElapsedTime(duration / 1000L)
            seekBar.isEnabled = seekAvailable
            seekBar.max = duration
            elapsedTime?.let {
                elapsedTimeView?.text = DateUtils.formatElapsedTime(it / 1000L)
                if (!scrubbing) seekBar.progress = it
            }
            if (seekBar is SquigglySeekBar) {
                seekBar.animate = playing && !scrubbing && listening
                seekBar.transitionEnabled = !seekAvailable
            }
        } else {
            seekBar.isEnabled = false
            seekBar.progress = 0
            seekBar.contentDescription = ""
            elapsedTimeView?.text = "00:00"
            totalTimeView?.text = "00:00"
            if (seekBar is SquigglySeekBar) {
                seekBar.animate = false
            }
        }
    }

    private fun getOrCreateRealSeekBar(holder: Any, isDynamicIsland: Boolean): SeekBar? {
        val existing = holder.getAdditionalInstanceField(KEY_REAL_SEEKBAR) as? SeekBar
        if (existing != null) return existing

        val mode = if (isDynamicIsland) diProgressMode else ncProgressMode
        val thickness = if (isDynamicIsland) diProgressThickness * 8 else ncProgressThickness
        val comet = if (isDynamicIsland) diProgressComet else ncProgressComet
        val thumb = if (isDynamicIsland) diCustomThumbStyle else ncCustomThumbStyle

        val origSeekBar = if (isDynamicIsland) {
            holder.getMediaViewHolderFieldAs<SeekBar>("seekBar", true)
        } else {
            holder.getObjectFieldOrNullAs<SeekBar>("seekBar")
        } ?: return null

        val parent = origSeekBar.parent as? ViewGroup ?: return null
        val context = origSeekBar.context
        val index = (parent.indexOfChild(origSeekBar) + 1).coerceIn(0, parent.childCount)

        val realSeekBar: SeekBar = when (mode) {
            1 -> {
                // SquigglySeekBar（波浪线进度条）
                SquigglySeekBar(context).apply {
                    id = mediaBgViewId
                    layoutParams = origSeekBar.layoutParams?.apply {
                        (this as? ViewGroup.MarginLayoutParams)?.updateMargins(top = 0, bottom = 0)
                    }
                    thumbStyle = thumb
                    waveLength = 20.dpFloat(context)
                    lineAmplitude = 1.5.dpFloat(context)
                    phaseSpeed = 8.dpFloat(context)
                    strokeWidth = 2.dpFloat(context)
                }
            }
            2 -> {
                // CometSeekBar（圆滑自定义进度条）
                val cornerRadius = if (isDynamicIsland) diCornerRadius else ncCornerRadius
                CometSeekBar(context).apply {
                    id = mediaBgViewId
                    layoutParams = origSeekBar.layoutParams?.apply {
                        (this as? ViewGroup.MarginLayoutParams)?.updateMargins(top = 0, bottom = 0)
                    }
                    progressHeight = thickness.dp
                    progressCornerRadius = cornerRadius.dp.toFloat()
                    cometEffect = comet
                    thumbStyle = thumb
                }
            }
            else -> return null
        }

        parent.addView(realSeekBar, index)
        parent.removeView(origSeekBar)
        holder.setAdditionalInstanceField(KEY_REAL_SEEKBAR, realSeekBar)
        XposedLog.d(TAG, lpparam.packageName, "getOrCreateRealSeekBar: created mode=$mode isDI=$isDynamicIsland")
        return realSeekBar
    }
}
