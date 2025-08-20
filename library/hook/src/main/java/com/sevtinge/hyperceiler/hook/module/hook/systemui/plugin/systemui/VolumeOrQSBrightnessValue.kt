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

package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.systemui

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
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.mSupportSV
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt.chooseBackgroundBlurContainer
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
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
import java.util.WeakHashMap

object VolumeOrQSBrightnessValue {
    private const val plugin = "miui.systemui.plugin"

    private val brightnessShow by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_qs_brightness_top_value_show")
    }
    private val volumeShow by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_qs_volume_top_value_show")
    }

    private val topTextCache = WeakHashMap<Any, TextView>()
    private val superVolumeCache = WeakHashMap<Any, TextView>()
    private val sliderSeekBarCache = WeakHashMap<Any, SeekBar>()
    private val controllerTopTextCache = WeakHashMap<Any, TextView>()
    private val colorArrayCache = HashMap<String, IntArray>()

    private var supportSV: Boolean = false

    private fun setTextIfChanged(tv: TextView, newText: String) {
        if (tv.text.toString() != newText) {
            tv.text = newText
        }
    }

    private fun setVisibilityIfNeeded(view: View, visible: Boolean) {
        val want = if (visible) View.VISIBLE else View.GONE
        if (view.visibility != want) view.visibility = want
    }

    private fun getValidCachedTextView(
        cache: WeakHashMap<Any, TextView>,
        key: Any,
        supplier: () -> TextView?
    ): TextView? {
        val cached = cache[key]
        if (cached != null) {
            try {
                if (cached.isAttachedToWindow && cached.parent != null) {
                    return cached
                }
            } catch (_: Throwable) {
            }
        }
        val fresh = try {
            supplier()
        } catch (_: Throwable) {
            null
        }
        if (fresh != null) {
            cache[key] = fresh
        }
        return fresh
    }

    fun initVolumeOrQSBrightnessValue(classLoader: ClassLoader) {
        if (!brightnessShow && !volumeShow) return

        supportSV = mSupportSV

        val controlCenterUtils = ControlCenterUtils(classLoader)
        val miBlurCompat = MiBlurCompat(classLoader)

        if (volumeShow) {
            val volumeUtils = loadClass("miui.systemui.util.VolumeUtils", classLoader)
            val volumeSliderController =
                loadClass("miui.systemui.controlcenter.panel.main.volume.VolumeSliderController", classLoader)
            val volumePanelViewController =
                loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
            val util = Util(classLoader)

            val progressToLevelCall: (Int, Int) -> Int = { max, value ->
                volumeUtils.callStaticMethod("progressToLevel", max, value) as Int
            }

            // 有超大音量功能的拦截隐藏
            if (supportSV) {
                volumePanelViewController
                    .methodFinder().filterByName("updateSuperVolumeView")
                    .first().replaceMethod {
                        null
                    }

                runCatching {
                    // 你米还是太抽象了
                    volumeSliderController
                        .methodFinder().filterByName("updateSuperVolume")
                        .first().createAfterHook { param ->
                            val sliderHolder = param.thisObject.callMethod("getHolder") ?: return@createAfterHook

                            val topValue = getValidCachedTextView(topTextCache, sliderHolder) {
                                val item = sliderHolder.getObjectField("itemView") as? View ?: return@getValidCachedTextView null
                                item.findViewByIdName("top_text") as? TextView
                            } ?: return@createAfterHook

                            val sliderMaxValue = param.thisObject.getObjectField("sliderMaxValue") as Int
                            val value = param.thisObject.callMethod("getTargetValue")!! as Int

                            val level = try {
                                progressToLevelCall(sliderMaxValue, value)
                            } catch (_: Throwable) {
                                value
                            }

                            val newText = convertToPercentageProgress(level, sliderMaxValue / 1000, 1)
                            setVisibilityIfNeeded(topValue, true)
                            setTextIfChanged(topValue, newText)
                        }
                }.onFailure {
                    logE("VolumeOrQSBrightnessValue", "updateSuperVolume replace failed: ${it.message}")
                }
            }

            // 控制中心音量条百分比进度值计算
            if (!supportSV) {
                volumeSliderController.methodFinder()
                    .filterByName("updateIconProgress")
                    .filterByParamTypes {
                        it[0] == Boolean::class.java
                    }.first().createAfterHook { param ->
                        val sliderHolder =
                            param.thisObject.callMethod("getHolder") ?: return@createAfterHook

                        val topValue = getValidCachedTextView(topTextCache, sliderHolder) {
                            val item = sliderHolder.getObjectField("itemView") as? View
                                ?: return@getValidCachedTextView null
                            item.findViewByIdName("top_text") as? TextView
                        } ?: return@createAfterHook

                        val sliderMaxValue =
                            param.thisObject.getObjectField("sliderMaxValue") as Int
                        val value = param.thisObject.callMethod("getTargetValue")!! as Int

                        val level = try {
                            progressToLevelCall(sliderMaxValue, value)
                        } catch (_: Throwable) {
                            value
                        }

                        val newText = convertToPercentageProgress(level, sliderMaxValue / 1000, 1)
                        setVisibilityIfNeeded(topValue, true)
                        setTextIfChanged(topValue, newText)
                    }
            }

            // 侧边音量条进度值 && All 场景二级音量条进度值 UI 启用
            volumePanelViewController.methodFinder()
                .filterByName("updateSuperVolumeView")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook { param ->
                    val thisObj = param.thisObject
                    val mExpanded = thisObj.getObjectField("mExpanded") as Boolean
                    val volumeColumn = param.args[0]
                    val superVolume = getValidCachedTextView(superVolumeCache, volumeColumn) {
                        (volumeColumn.getObjectField("superVolume") as? TextView)
                    } ?: return@createAfterHook
                    val mSuperVolumeBg = thisObj.getObjectField("mSuperVolumeBg") as View

                    util.setVisOrGone(mSuperVolumeBg, !mExpanded) // 侧边音量条进度值 UI 显示
                    util.setVisOrGone(superVolume, mExpanded) // All 场景二级音量条进度值 UI 显示
                }

            // All 场景二级音量条百分比进度值计算显示
            volumePanelViewController.methodFinder()
                .filterByName("updateVolumeColumnSliderH")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook { param ->
                    val volumeColumn = param.args[0]
                    val mState = param.thisObject.getObjectField("mState")
                    val states = mState?.getObjectField("states") as? SparseArray<*>
                    val stream = volumeColumn.getObjectField("stream") as Int
                    val streamState = states?.get(stream)
                    val mActiveStream = param.thisObject.getObjectField("mActiveStream") as Int

                    if (streamState != null) {
                        val maxLevel = streamState.getObjectField("levelMax") as Int
                        val level = streamState.getObjectField("level") as Int

                        val superVolumeTv = getValidCachedTextView(superVolumeCache, volumeColumn) {
                            (volumeColumn.getObjectField("superVolume") as? TextView)
                        } ?: return@createAfterHook
                        val newText = convertToPercentageProgress(level, maxLevel, 1)
                        setTextIfChanged(superVolumeTv, newText)

                        if (stream == mActiveStream) {
                            val mSuperVolumeTv = param.thisObject.getObjectField("mSuperVolume") as TextView
                            setTextIfChanged(mSuperVolumeTv, newText)
                        }
                    }
                }

            // 为 All 二级进度值适配高级材质
            volumePanelViewController.methodFinder()
                .filterByName("updateColumnSliderBlendColor")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook { param ->
                    val thisObj = param.thisObject
                    val volumeColumn = param.args[0]
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
                    val superVolume = getValidCachedTextView(superVolumeCache, volumeColumn) {
                        (volumeColumn.getObjectField("superVolume") as? TextView)
                    } ?: return@createAfterHook
                    superVolume.setTextColor(Color.WHITE)
                    val cacheKey = "$plugin:$colorArrayName"
                    val colorArray = colorArrayCache.getOrPut(cacheKey) {
                        mContext.resources.getIntArrayBy(colorArrayName, plugin)
                    }
                    util.setMiViewBlurAndBlendColor(superVolume, mExpanded, mContext, 3, colorArray, false)
                }

        }

        if (brightnessShow) {
            val brightnessSliderController = loadClass(
                "miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController",
                classLoader
            )
            val brightnessPanelSliderController = loadClass(
                "miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController",
                classLoader
            )

            // 控制中心一级亮度条计算
            brightnessSliderController.methodFinder()
                .filterByName("updateIconProgress")
                .first().createAfterHook { param ->
                    val sliderHolder = param.thisObject.callMethod("getSliderHolder") ?: return@createAfterHook

                    val topValue = getValidCachedTextView(topTextCache, sliderHolder) {
                        val item = sliderHolder.getObjectField("itemView") as? View ?: return@getValidCachedTextView null
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook

                    val seekBar = sliderSeekBarCache.getOrPut(param.thisObject) {
                        param.thisObject.callMethod("getSlider") as SeekBar
                    }
                    val newText = convertToPercentageProgress(seekBar.progress, seekBar.max, 2)
                    setVisibilityIfNeeded(topValue, true)
                    setTextIfChanged(topValue, newText)
                }

            // 控制中心二级亮度条百分比进度值计算
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateIconProgress")
                .first().createAfterHook { param ->
                    val controller = param.thisObject
                    val vToggleSliderInner = controller.callMethod("getVToggleSliderInner") as ViewGroup
                    val seekBar = sliderSeekBarCache.getOrPut(controller) {
                        controller.callMethod("getVSlider") as SeekBar
                    }
                    val topValue = getValidCachedTextView(topTextCache, vToggleSliderInner) {
                        vToggleSliderInner.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook

                    setVisibilityIfNeeded(topValue, true)
                    setTextIfChanged(topValue, convertToPercentageProgress(seekBar.progress, seekBar.max, 2))
                }

            // 控制中心二级亮度条进度值高级材质适配
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateBlendBlur")
                .first().createAfterHook { param ->
                    val context = param.thisObject.callMethod("getContext") as Context
                    val vToggleSliderInner = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = getValidCachedTextView(topTextCache, vToggleSliderInner) {
                        vToggleSliderInner.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook

                    if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                        val color = vToggleSliderInner.resources.getColorBy("toggle_slider_top_text_color", plugin)
                        topValue.setTextColor(color)
                        miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                        miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                        return@createAfterHook
                    }
                    topValue.setTextColor(Color.WHITE)
                    miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                    val cacheKey = "$plugin:toggle_slider_icon_blend_colors"
                    val colorArray = colorArrayCache.getOrPut(cacheKey) {
                        vToggleSliderInner.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                    }
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
                }

            // 设置展开的大小
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateLargeSize")
                .first().createAfterHook { param ->
                    val item = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = getValidCachedTextView(topTextCache, item) {
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook
                    topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                }

            // 设置未展开的大小
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateSmallSize")
                .first().createAfterHook { param ->
                    val item = param.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = getValidCachedTextView(topTextCache, item) {
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook
                    topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                }

            val brightnessPanelAnimator =
                loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelAnimator", classLoader)

            // 修复过渡动画错位，并增加大小过渡动画
            brightnessPanelAnimator.methodFinder()
                .filterByName("frameCallback")
                .first().createAfterHook { param ->
                    val sliderController = param.thisObject.getObjectField("sliderController") ?: return@createAfterHook
                    val topValue = getValidCachedTextView(controllerTopTextCache, sliderController) {
                        val item = sliderController.callMethod("getVToggleSliderInner") as? ViewGroup ?: return@getValidCachedTextView null
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook
                    val icon = sliderController.callMethod("getVIcon") as View

                    val sizeBgX = param.thisObject.getObjectFieldAs<Float>("sizeBgX")
                    val left = (dpToPx(50f, topValue.resources.displayMetrics) - icon.layoutParams.width).toInt() / 2
                    topValue.left = icon.left - left
                    topValue.right = icon.right + left
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f + 2f * sizeBgX)
                }

            // 修复展开动画错位之一
            brightnessPanelSliderController.constructorFinder()
                .toList().createAfterHooks { param ->
                    val brightnessPanel = param.args[0] as FrameLayout

                    val topText = brightnessPanel.findViewByIdName("top_text") as TextView
                    topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    val mLayoutParams =
                        (topText.layoutParams as ViewGroup.LayoutParams).apply {
                            width = dpToPx(50f, topText.resources.displayMetrics).toInt()
                        }
                    topText.layoutParams = mLayoutParams
                }

            // 貌似用不到的修复展开动画错位的方法之一
            brightnessSliderController.methodFinder()
                .filterByName("createViewHolder")
                .filterByParamTypes {
                    it[0] == ViewGroup::class.java && it[1] == Int::class.java
                }.first().createAfterHook { param ->
                    val viewHolder = param.result

                    if (viewHolder != null) {
                        val root = viewHolder.getObjectFieldAs<ViewGroup>("itemView")
                        val topText = root.findViewByIdName("top_text") as TextView
                        topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val mLayoutParams =
                            (topText.layoutParams as ViewGroup.LayoutParams).apply {
                                width = dpToPx(50f, root.resources.displayMetrics).toInt()
                            }
                        topText.layoutParams = mLayoutParams
                    }
                }

        }

        // 控制中心非展开状态下的进度值高级材质适配
        val toggleSliderViewHolder =
            loadClass("miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder", classLoader)

        toggleSliderViewHolder.methodFinder().apply {
            filterByName("updateSize")
                .first().createAfterHook { param ->
                    val item = param.thisObject.getObjectField("itemView") as View
                    val topValue = getValidCachedTextView(topTextCache, item) {
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook
                    topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP,13f)

                }
            filterByName("updateBlendBlur")
                .first().createAfterHook { param ->
                    val context = param.thisObject.callMethod("getContext") as Context

                    val item = param.thisObject.getObjectField("itemView") as View
                    val topValue = getValidCachedTextView(topTextCache, item) {
                        item.findViewByIdName("top_text") as? TextView
                    } ?: return@createAfterHook

                    if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                        val colorId = context.resources.getIdentifier(
                            "toggle_slider_top_text_color",
                            "color",
                            "miui.systemui.plugin"
                        )
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
                        } else {
                            null
                        }
                    )

                    miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                    val cacheKey = "$plugin:toggle_slider_icon_blend_colors"
                    val colorArray: IntArray =
                        colorArrayCache.getOrPut(cacheKey) {
                            context.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                        }
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)

                }
        }
    }

    private fun convertToPercentageProgress(
        progress: Int,
        max: Int,
        progressbar: Int
    ): String {
        val value = progress * 100 / max
        val result = if (supportSV && value >= 100 && progressbar == 1) {
            "100%+" // 因为有 200% 300% 400% 的显示逻辑，不想搞，统一 100%+
        } else {
            "$value%"
        }
        return result
    }


    class ControlCenterUtils(classLoader: ClassLoader?) {

        private val controlCenterUtils =
            loadClass("miui.systemui.controlcenter.utils.ControlCenterUtils", classLoader)

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

        fun setVisOrGone(
            view: View,
            visOrGone: Boolean
        ) {
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
