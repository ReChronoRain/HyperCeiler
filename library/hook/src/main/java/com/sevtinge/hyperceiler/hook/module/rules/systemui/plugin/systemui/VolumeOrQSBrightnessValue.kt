/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.util.TypedValueCompat.dpToPx
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.api.mSupportSV
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt.chooseBackgroundBlurContainer
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import de.robv.android.xposed.XposedHelpers.getStaticBooleanField
import io.github.kyuubiran.ezxhelper.android.util.ViewUtil.findViewByIdName
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHooks
import java.lang.reflect.Method
import java.util.WeakHashMap

object VolumeOrQSBrightnessValue {
    private const val PLUGIN = "miui.systemui.plugin"

    private val brightnessShow by lazy { mPrefsMap.getBoolean("system_ui_control_center_qs_brightness_top_value_show") }
    private val volumeShow by lazy { mPrefsMap.getBoolean("system_ui_control_center_qs_volume_top_value_show") }

    private val topTextInitialized = WeakHashMap<Any, Boolean>()
    private val topTextCache = WeakHashMap<Any, TextView>()
    private val superVolumeCache = WeakHashMap<Any, TextView>()
    private val sliderSeekBarCache = WeakHashMap<Any, SeekBar>()
    private val controllerTopTextCache = WeakHashMap<Any, TextView>()
    private val streamCache = WeakHashMap<Any, Int>()

    private val colorArrayCache = HashMap<String, IntArray>()
    private var progressToLevelMethod: Method? = null

    private fun setTextIfChanged(tv: TextView, newText: String) {
        if (tv.text.toString() != newText) tv.text = newText
    }

    private fun normalizeTopTextViewLayout(tv: TextView, size: Float) {
        try {
            if (topTextInitialized[tv] == true) return
            topTextInitialized[tv] = true
            tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
            val desiredWidth = dpToPx(50f, tv.resources.displayMetrics).toInt()
            val lp = tv.layoutParams?.apply {
                if (width != desiredWidth) width = desiredWidth
            } ?: ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            tv.layoutParams = lp
        } catch (_: Throwable) {
        }
    }

    private fun getOrCacheTopText(
        cache: MutableMap<Any, TextView>,
        holder: Any,
        itemViewProvider: () -> View?,
        contextProvider: () -> Context
    ): TextView {
        val cached = cache[holder]
        if (cached != null && cached.isAttachedToWindow) return cached
        val item = itemViewProvider()
        val tv = (item?.findViewByIdName("top_text") as? TextView) ?: TextView(contextProvider())
        cache[holder] = tv
        return tv
    }

    private fun updateTopText(
        tv: TextView,
        text: String,
        size: Float = 13f,
        visible: Boolean = true,
        normalize: Boolean = true
    ) {
        runCatching {
            if (normalize) normalizeTopTextViewLayout(tv, size)
            val wantVis = if (visible) View.VISIBLE else View.GONE
            if (tv.visibility != wantVis) tv.visibility = wantVis
            setTextIfChanged(tv, text)
        }.onFailure {
            logE("VolumeOrQSBrightnessValue", "updateTopText failed: ${it.stackTraceToString()}")
        }
    }

    private fun getOrCacheStream(volumeColumn: Any): Int {
        streamCache[volumeColumn]?.let { return it }
        val stream = try {
            volumeColumn.getObjectFieldOrNullAs<Int>("stream")
                ?: (volumeColumn.callMethod("getStream") as? Int)
                ?: -1
        } catch (_: Throwable) {
            -1
        }
        streamCache[volumeColumn] = stream
        return stream
    }

    fun initVolumeOrQSBrightnessValue(classLoader: ClassLoader) {
        val controlCenterUtils = ControlCenterUtils(classLoader)
        val miBlurCompat = MiBlurCompat(classLoader)

        if (volumeShow) setupVolumeHooks(classLoader)
        if (brightnessShow) setupBrightnessHooks(classLoader, controlCenterUtils, miBlurCompat)

        // 控制中心非展开状态下的进度值高级材质适配
        setupToggleSliderViewHolderHooks(classLoader, controlCenterUtils, miBlurCompat)
    }

    private fun setupVolumeHooks(classLoader: ClassLoader) {
        val volumeUtils = loadClass("miui.systemui.util.VolumeUtils", classLoader)
        val volumeSliderController = loadClass("miui.systemui.controlcenter.panel.main.volume.VolumeSliderController", classLoader)

        runCatching {
            progressToLevelMethod = volumeUtils.getMethod("progressToLevel", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        }.onFailure {
            logE("VolumeOrQSBrightnessValue", "cache progressToLevel method failed: ${it.message}")
            progressToLevelMethod = null
        }

        val progressToLevelCall: (Int, Int) -> Int = { max, value ->
            progressToLevelMethod?.let { m ->
                try {
                    (m.invoke(null, max, value) as? Int) ?: value
                } catch (_: Throwable) {
                    value
                }
            } ?: value
        }

        // 控制中心音量条百分比进度值计算
        val sliderUpdateFinder = try {
            if (mSupportSV) {
                // 你米还是太抽象了
                volumeSliderController.methodFinder().filterByName("updateSuperVolume")
            } else {
                volumeSliderController.methodFinder().filterByName("updateIconProgress")
            }
        } catch (_: Throwable) {
            volumeSliderController.methodFinder().filterByName("updateIconProgress")
        }

        sliderUpdateFinder.first().createAfterHook { param ->
            val sliderHolder = param.thisObject.callMethod("getHolder") ?: return@createAfterHook
            val topValue = getOrCacheTopText(
                topTextCache,
                sliderHolder,
                { sliderHolder.getObjectFieldOrNullAs<View>("itemView") },
                { param.thisObject.getObjectField("mContext") as Context }
            )
            if (!topValue.isAttachedToWindow) {
                runCatching {
                    val item = sliderHolder.getObjectFieldOrNullAs<View>("itemView")
                    val newTv = item?.findViewByIdName("top_text") as? TextView
                    if (newTv != null) topTextCache[sliderHolder] = newTv
                }
            }
            val effectiveTop = topTextCache[sliderHolder] ?: return@createAfterHook
            if (!effectiveTop.isAttachedToWindow) return@createAfterHook

            val sliderMaxValue = param.thisObject.getObjectFieldOrNullAs<Int>("sliderMaxValue") ?: return@createAfterHook
            val value = param.thisObject.callMethod("getTargetValue") as? Int ?: return@createAfterHook

            val level = try {
                progressToLevelCall(sliderMaxValue, value)
            } catch (_: Throwable) {
                value
            }

            val percent = sliderMaxValue / 1000
            val newText = convertToPercentageVolumeProgress(level, percent)
            updateTopText(effectiveTop, newText)
        }

        // 侧边音量条进度值 && All 场景二级音量条进度值 UI 启用
        val volumePanelViewController = loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
        val util = Util(classLoader)

        volumePanelViewController.methodFinder().apply {
            filterByName("updateSuperVolumeView").first().createAfterHook { param ->
                val thisObj = param.thisObject
                val mExpanded = thisObj.getObjectFieldOrNullAs<Boolean>("mExpanded") ?: false
                val volumeColumn = param.args[0] ?: return@createAfterHook

                // 获取或刷新 superVolume TextView
                val superVolume = try {
                    val cached = superVolumeCache[volumeColumn]
                    if (cached != null && cached.isAttachedToWindow) cached
                    else {
                        val found = volumeColumn.getObjectFieldOrNullAs<TextView>("superVolume")
                        if (found != null) superVolumeCache[volumeColumn] = found
                        found
                    }
                } catch (e: Throwable) {
                    logE("VolumeOrQSBrightnessValue", "get superVolume failed: ${e.message}")
                    null
                } ?: return@createAfterHook

                val mSuperVolumeBg = thisObj.getObjectFieldOrNullAs<View>("mSuperVolumeBg") ?: return@createAfterHook
                normalizeTopTextViewLayout(superVolume, 13f)

                util.setVisOrGone(mSuperVolumeBg, !mExpanded) // 侧边音量条进度值 UI 显示
                util.setVisOrGone(superVolume, mExpanded) // All 场景二级音量条进度值 UI 显示

                /*if (isMoreHyperOSVersion(3f)) {
                    // 为 All 二级进度值适配高级材质
                    val mContext = thisObj.getObjectField("mContext") as Context

                    superVolume.setTextColor(Color.WHITE)
                    val cacheKey = "$PLUGIN:miui_volume_icon_blend_colors_cc"
                    val colorArray = colorArrayCache.getOrPut(cacheKey) {
                        mContext.resources.getIntArrayBy("miui_volume_icon_blend_colors_cc", PLUGIN)
                    }
                    util.setMiViewBlurAndBlendColor(superVolume, mExpanded, mContext, 3, colorArray, false)
                }*/
            }

            // All 场景二级音量条百分比进度值计算显示
            filterByName("updateVolumeColumnSliderH").first().createAfterHook { param ->
                val volumeColumn = param.args[0] ?: return@createAfterHook
                val mState = param.thisObject.getObjectFieldOrNullAs<Any>("mState")
                val states = mState?.getObjectFieldOrNullAs<SparseArray<*>>("states")

                // 使用缓存获取 stream，避免频繁反射
                val stream = getOrCacheStream(volumeColumn)
                val streamState = states?.get(stream)
                val mActiveStream = param.thisObject.getObjectFieldOrNullAs<Int>("mActiveStream") ?: -1

                if (streamState != null) {
                    val maxLevel = streamState.getObjectFieldOrNullAs<Int>("levelMax") ?: return@createAfterHook
                    val level = streamState.getObjectFieldOrNullAs<Int>("level") ?: return@createAfterHook

                    val superVolumeTv = runCatching {
                        val cached = superVolumeCache[volumeColumn]
                        if (cached != null && cached.isAttachedToWindow) cached
                        else {
                            val found = volumeColumn.getObjectFieldOrNullAs<TextView>("superVolume")
                            if (found != null) superVolumeCache[volumeColumn] = found
                            found
                        }
                    }.getOrNull() ?: return@createAfterHook

                    val newText = convertToPercentageVolumeProgress(level, maxLevel)
                    setTextIfChanged(superVolumeTv, newText)

                    if (stream == mActiveStream) {
                        val mSuperVolumeTv = param.thisObject.getObjectFieldOrNullAs<TextView>("mSuperVolume")
                        mSuperVolumeTv?.let { setTextIfChanged(it, newText) }
                    }
                }
            }

            // 为 All 二级进度值适配高级材质
            if (!isMoreHyperOSVersion(3f)) {
                filterByName("updateColumnSliderBlendColor").first().createAfterHook { param ->
                    val thisObj = param.thisObject
                    val volumeColumn = param.args[0] ?: return@createAfterHook
                    val mContext = thisObj.getObjectField("mContext") as Context
                    val mExpanded = thisObj.getObjectField("mExpanded") as Boolean
                    val mNeedShowDialog = thisObj.getObjectField("mNeedShowDialog") as Boolean
                    val colorArrayName = if (!mExpanded) {
                        "miui_expanded_button_and_seekbar_icon_blend_colors_collapsed"
                    } else if (mNeedShowDialog) {
                        "miui_seekbar_icon_blend_colors_expanded"
                    } else {
                        "miui_seekbar_icon_blend_colors_expanded_cc"
                    }

                    val superVolume = try {
                        val cached = superVolumeCache[volumeColumn]
                        if (cached != null && cached.isAttachedToWindow) cached
                        else {
                            val found = volumeColumn.getObjectFieldOrNullAs<TextView>("superVolume")
                            if (found != null) superVolumeCache[volumeColumn] = found
                            found
                        }
                    } catch (e: Throwable) {
                        logE("VolumeOrQSBrightnessValue", "get superVolume in blend color failed: ${e.message}")
                        null
                    } ?: return@createAfterHook

                    superVolume.setTextColor(Color.WHITE)
                    val cacheKey = "$PLUGIN:$colorArrayName"
                    val colorArray = colorArrayCache.getOrPut(cacheKey) {
                        mContext.resources.getIntArrayBy(colorArrayName, PLUGIN)
                    }
                    util.setMiViewBlurAndBlendColor(superVolume, mExpanded, mContext, 3, colorArray, false)
                }
            }
        }

        // 有超大音量功能的拦截隐藏
        if (mSupportSV) {
            volumePanelViewController.methodFinder().filterByName("updateSuperVolumeView").first().replaceMethod { null }
        }
    }

    private fun setupBrightnessHooks(
        classLoader: ClassLoader,
        controlCenterUtils: ControlCenterUtils,
        miBlurCompat: MiBlurCompat
    ) {
        val brightnessSliderController = loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController", classLoader)
        val brightnessPanelSliderController = if (isMoreHyperOSVersion(3f)) {
            loadClass("miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelSliderDelegate", classLoader)
        } else {
            loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController", classLoader)
        }

        // 控制中心一级亮度条计算
        brightnessSliderController.methodFinder().filterByName("updateIconProgress").first().createAfterHook { param ->
            val sliderHolder = param.thisObject.callMethod("getSliderHolder") ?: return@createAfterHook
            val topValue = getOrCacheTopText(
                topTextCache,
                sliderHolder,
                { (sliderHolder.getObjectField("itemView") as? View) },
                { (sliderHolder.getObjectField("itemView") as View).context }
            )
            val seekBar = sliderSeekBarCache.getOrPut(param.thisObject) { param.thisObject.callMethod("getSlider") as SeekBar }
            val newText = convertToPercentageBrightProgress(seekBar.progress, seekBar.max)
            updateTopText(topValue, newText, visible = true, normalize = false)
        }

        // 控制中心二级亮度条百分比进度值计算
        brightnessPanelSliderController.methodFinder().filterByName("updateIconProgress").first().createAfterHook { param ->
            val controller = param.thisObject
            val vToggleSliderInner = controller.callMethod("getVToggleSliderInner") as ViewGroup
            val seekBar = sliderSeekBarCache.getOrPut(controller) { controller.callMethod("getVSlider") as SeekBar }
            val topValue = getOrCacheTopText(
                topTextCache,
                vToggleSliderInner,
                { vToggleSliderInner },
                { vToggleSliderInner.context }
            )
            val newText = convertToPercentageBrightProgress(seekBar.progress, seekBar.max)
            updateTopText(topValue, newText, visible = true, normalize = false)
        }

        // 控制中心二级亮度条进度值高级材质适配
        brightnessPanelSliderController.methodFinder().filterByName("updateBlendBlur").first().createAfterHook { param ->
            val context = param.thisObject.callMethod("getContext") as Context
            val vToggleSliderInner = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
            val topValue = vToggleSliderInner.findViewByIdName("top_text") as TextView

            if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                val color = vToggleSliderInner.resources.getColorBy("toggle_slider_top_text_color", PLUGIN)
                topValue.setTextColor(color)
                miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                return@createAfterHook
            }
            topValue.setTextColor(Color.WHITE)
            miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

            if (isMoreHyperOSVersion(3f)) {
                val cacheKey = "$PLUGIN:miui_volume_icon_blend_colors_cc"
                val colorArray: IntArray = colorArrayCache.getOrPut(cacheKey) {
                    context.resources.getIntArrayBy("miui_volume_icon_blend_colors_cc", PLUGIN)
                }
                miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
            } else {
                val cacheKey = "$PLUGIN:toggle_slider_icon_blend_colors"
                val colorArray: IntArray = colorArrayCache.getOrPut(cacheKey) {
                    context.resources.getIntArrayBy("toggle_slider_icon_blend_colors", PLUGIN)
                }
                miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
            }
        }

        // 设置展开的大小
        brightnessPanelSliderController.methodFinder().filterByName("updateLargeSize").first().createAfterHook { param ->
            val item = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
            val topValue = item.findViewByIdName("top_text") as TextView
            normalizeTopTextViewLayout(topValue, 15f)
        }

        // 设置未展开的大小
        brightnessPanelSliderController.methodFinder().filterByName("updateSmallSize").first().createAfterHook { param ->
            val item = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
            val topValue = item.findViewByIdName("top_text") as TextView
            normalizeTopTextViewLayout(topValue, 13f)
        }

        val brightnessPanelAnimator = if (isMoreHyperOSVersion(3f)) {
            loadClass("miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelAnimator", classLoader)
        } else {
            loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelAnimator", classLoader)
        }

        // 修复过渡动画错位，并增加大小过渡动画
        brightnessPanelAnimator.methodFinder().filterByName("frameCallback").first().createAfterHook { param ->
            val sliderController = param.thisObject.getObjectFieldOrNull("sliderController")
                ?: param.thisObject.getObjectFieldOrNull("sliderDelegate")
                ?: return@createAfterHook

            val topValue = controllerTopTextCache.getOrPut(sliderController) {
                val item = sliderController.callMethod("getVToggleSliderInner") as? ViewGroup
                    ?: throw IllegalStateException("no vToggleSliderInner")
                item.findViewByIdName("top_text") as TextView
            }

            val icon = sliderController.callMethod("getVIcon") as View
            val sizeBgX = param.thisObject.getObjectFieldOrNullAs<Float>("sizeBgX")
                ?: param.thisObject.getObjectFieldOrNullAs<Float>("size")
                ?: return@createAfterHook
            val left = (dpToPx(50f, topValue.resources.displayMetrics) - icon.layoutParams.width).toInt() / 2
            topValue.left = icon.left - left
            topValue.right = icon.right + left
            topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f + 2f * sizeBgX)
        }

        if (!isMoreAndroidVersion(36)) {
            // 修复展开动画错位之一
            brightnessPanelSliderController.constructorFinder().toList().createAfterHooks { param ->
                val brightnessPanel = param.args.getOrNull(0) as? FrameLayout ?: return@createAfterHooks
                val topText = brightnessPanel.findViewByIdName("top_text") as? TextView ?: return@createAfterHooks
                topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                val mLayoutParams = (topText.layoutParams as ViewGroup.LayoutParams).apply {
                    width = dpToPx(50f, topText.resources.displayMetrics).toInt()
                }
                topText.layoutParams = mLayoutParams
            }

            // 貌似用不到的修复展开动画错位的方法之一
            brightnessSliderController.methodFinder()
                .filterByName("createViewHolder")
                .filterByParamTypes { it[0] == ViewGroup::class.java && it[1] == Int::class.java }
                .first().createAfterHook { param ->
                    val viewHolder = param.result
                    if (viewHolder != null) {
                        val root = viewHolder.getObjectFieldAs<ViewGroup>("itemView")
                        val topText = root.findViewByIdName("top_text") as TextView
                        topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val mLayoutParams = (topText.layoutParams as ViewGroup.LayoutParams).apply {
                            width = dpToPx(50f, root.resources.displayMetrics).toInt()
                        }
                        topText.layoutParams = mLayoutParams
                    }
                }
        }
    }

    private fun setupToggleSliderViewHolderHooks(
        classLoader: ClassLoader,
        controlCenterUtils: ControlCenterUtils,
        miBlurCompat: MiBlurCompat
    ) {
        val toggleSliderViewHolder = loadClass("miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder", classLoader)

        toggleSliderViewHolder.methodFinder().apply {
            filterByName("updateSize").first().createAfterHook { param ->
                val item = param.thisObject.getObjectField("itemView") as View
                val topValue = getOrCacheTopText(topTextCache, item, { item }, { item.context })
                topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            }

            filterByName("updateBlendBlur").first().createAfterHook { param ->
                val context = param.thisObject.callMethod("getContext") as Context
                val item = param.thisObject.getObjectField("itemView") as View
                val topValue = getOrCacheTopText(topTextCache, item, { item }, { item.context })

                if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                    val colorId = context.resources.getIdentifier("toggle_slider_top_text_color", "color", PLUGIN)
                    val color = item.resources.getColor(colorId, null)
                    topValue.setTextColor(color)
                    miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                    miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                    return@createAfterHook
                }

                topValue.setTextColor(Color.WHITE)
                val inMirror = param.thisObject.getObjectField("inMirror") as Boolean
                topValue.chooseBackgroundBlurContainer(
                    if (inMirror) {
                        with(param.thisObject) {
                            getObjectFieldOrNullAs<View>("mirrorBlendBackground")
                                ?: getObjectFieldOrNullAs<View>("mirrorBlurProvider")
                        }
                    } else null
                )
                miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                if (isMoreHyperOSVersion(3f)) {
                    val cacheKey = "$PLUGIN:miui_volume_icon_blend_colors_cc"
                    val colorArray: IntArray = colorArrayCache.getOrPut(cacheKey) {
                        context.resources.getIntArrayBy("miui_volume_icon_blend_colors_cc", PLUGIN)
                    }
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
                } else {
                    val cacheKey = "$PLUGIN:toggle_slider_icon_blend_colors"
                    val colorArray: IntArray = colorArrayCache.getOrPut(cacheKey) {
                        context.resources.getIntArrayBy("toggle_slider_icon_blend_colors", PLUGIN)
                    }
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
                }
            }
        }
    }

    private fun convertToPercentageVolumeProgress(progress: Int, max: Int): String {
        val value = progress * 100 / max
        return if (mSupportSV && value >= 100) "100%+" else "$value%"
    }

    private fun convertToPercentageBrightProgress(progress: Int, max: Int): String {
        val percent = (progress * 100 / max)
        return "${percent}%"
    }

    class ControlCenterUtils(classLoader: ClassLoader?) {
        private val controlCenterUtils = loadClass("miui.systemui.controlcenter.utils.ControlCenterUtils", classLoader)
        fun getBackgroundBlurOpenedInDefaultTheme(context: Context) =
            controlCenterUtils.callStaticMethod("getBackgroundBlurOpenedInDefaultTheme", context) as Boolean
    }

    class MiBlurCompat(classLoader: ClassLoader?) {
        private val miBlurCompat = loadClass("miui.systemui.util.MiBlurCompat", classLoader)
        fun setMiViewBlurModeCompat(view: View, mode: Int) {
            miBlurCompat.callStaticMethod("setMiViewBlurModeCompat", view, mode)
        }
        fun clearMiBackgroundBlendColorCompat(view: View) {
            miBlurCompat.callStaticMethod("clearMiBackgroundBlendColorCompat", view)
        }
        fun setMiBackgroundBlendColors(view: View?, colorArray: IntArray, float: Float) {
            miBlurCompat.callStaticMethod("setMiBackgroundBlendColors", view, colorArray, float)
        }
    }

    class Util(classLoader: ClassLoader?) {
        private val util = loadClass("com.android.systemui.miui.volume.Util", classLoader)
        val DEBUG = getStaticBooleanField(util, "DEBUG")

        fun setMiViewBlurAndBlendColor(
            view: View,
            z: Boolean,
            context: Context,
            i: Int,
            iArr: IntArray,
            z2: Boolean
        ) {
            util.callStaticMethod("setMiViewBlurAndBlendColor", view, z, context, i, iArr, z2)
        }

        fun setVisOrGone(view: View, visOrGone: Boolean) {
            util.callStaticMethod("setVisOrGone", view, visOrGone)
        }
    }

    private fun Resources.getColorBy(name: String, defPackage: String): Int {
        val id = this.getIdentifier(name, "color", defPackage)
        return this.getColor(id, this.newTheme())
    }

    private fun Resources.getIntArrayBy(name: String, defPackage: String): IntArray {
        val id = this.getIdentifier(name, "array", defPackage)
        return this.getIntArray(id)
    }
}
