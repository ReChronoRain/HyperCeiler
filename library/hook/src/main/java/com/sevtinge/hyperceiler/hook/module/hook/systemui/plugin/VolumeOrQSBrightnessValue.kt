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

package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin

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
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHooks
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.mSupportSV
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.getStaticBooleanField

object VolumeOrQSBrightnessValue {
    private const val plugin = "miui.systemui.plugin"

    private val brightnessShow by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_qs_brightness_top_value_show")
    }
    private val volumeShow by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_qs_volume_top_value_show")
    }

    fun initVolumeOrQSBrightnessValue(classLoader: ClassLoader) {
        if (!brightnessShow && !volumeShow) return

        val controlCenterUtils = ControlCenterUtils(classLoader)
        val miBlurCompat = MiBlurCompat(classLoader)

        if (volumeShow) {
            val volumeUtils = loadClass("miui.systemui.util.VolumeUtils", classLoader)
            val volumeSliderController = loadClass(
                "miui.systemui.controlcenter.panel.main.volume.VolumeSliderController",
                classLoader
            )

            // 控制中心音量条百分比进度值计算
            volumeSliderController.methodFinder()
                .filterByName("updateIconProgress")
                .filterByParamTypes {
                    it[0] == Boolean::class.java
                }.first().createAfterHook {
                    val sliderHolder = it.thisObject.callMethod("getHolder") ?: return@createAfterHook
                    val item = sliderHolder.getObjectField("itemView") as View
                    val topValue = item.findViewByIdName("top_text") as TextView
                    val sliderMaxValue = it.thisObject.getObjectField("sliderMaxValue") as Int
                    val value = it.thisObject.callMethod("getTargetValue")!! as Int
                    val level = volumeUtils.callStaticMethod("progressToLevel", sliderMaxValue, value) as Int

                    topValue.visibility = View.VISIBLE
                    topValue.text = convertToPercentageProgress(level, sliderMaxValue / 1000)
                }

            val volumePanelViewController =
                loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
            val util = Util(classLoader)

            // 侧边音量条进度值 && All 场景二级音量条进度值 UI 启用
            volumePanelViewController.methodFinder()
                .filterByName("updateSuperVolumeView")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook {
                    val mExpanded = it.thisObject.getObjectField("mExpanded") as Boolean
                    val superVolume = it.args[0].getObjectField("superVolume") as TextView
                    val mSuperVolumeBg = it.thisObject.getObjectField("mSuperVolumeBg") as View

                    util.setVisOrGone(mSuperVolumeBg, !mExpanded) // 侧边音量条进度值 UI 显示
                    util.setVisOrGone(superVolume, mExpanded) // All 场景二级音量条进度值 UI 显示
                }

            // All 场景二级音量条百分比进度值计算显示
            volumePanelViewController.methodFinder()
                .filterByName("updateVolumeColumnSliderH")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook {
                    val volumeColumn = it.args[0]
                    val mState = it.thisObject.getObjectField("mState")
                    val states = mState?.getObjectField("states") as SparseArray<*>
                    val stream = volumeColumn.getObjectField("stream") as Int
                    val streamState = states.get(stream)
                    val mActiveStream = it.thisObject.getObjectField("mActiveStream") as Int

                    if (streamState != null) {
                        val maxLevel = streamState.getObjectField("levelMax") as Int
                        val level = streamState.getObjectField("level") as Int

                        (volumeColumn.getObjectField("superVolume") as TextView).text =
                            convertToPercentageProgress(level, maxLevel)

                        if (stream == mActiveStream) {
                            (it.thisObject.getObjectField("mSuperVolume") as TextView).text =
                                convertToPercentageProgress(level, maxLevel)
                        }
                    }
                }

            // 为 All 二级进度值适配高级材质
            volumePanelViewController.methodFinder()
                .filterByName("updateColumnSliderBlendColor")
                .filterByParamTypes {
                    it[0] == loadClass("com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn", classLoader)
                }.first().createAfterHook {
                    val thisObj = it.thisObject
                    val volumeColumn = it.args[0]
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
                    val superVolume = volumeColumn.getObjectField("superVolume") as TextView
                    superVolume.setTextColor(Color.WHITE)
                    val colorArray = mContext.resources.getIntArrayBy(colorArrayName, plugin)
                    util.setMiViewBlurAndBlendColor(superVolume, mExpanded, mContext, 3, colorArray, false)
                }

            // 有超大音量功能的拦截隐藏
            if (mSupportSV) {
                volumePanelViewController
                    .methodFinder().filterByName("updateSuperVolumeView")
                    .first().replaceMethod {
                        null
                    }
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
                .first().createAfterHook {
                    val sliderHolder = it.thisObject.callMethod("getSliderHolder") ?: return@createAfterHook

                    val item = sliderHolder.getObjectField("itemView") as View
                    val seekBar = it.thisObject.callMethod("getSlider") as SeekBar
                    val topValue = item.findViewByIdName("top_text") as TextView

                    topValue.visibility = View.VISIBLE
                    topValue.text = convertToPercentageProgress(seekBar.progress, seekBar.max)
                }

            // 控制中心二级亮度条百分比进度值计算
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateIconProgress")
                .first().createAfterHook {
                    val vToggleSliderInner = it.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val seekBar = it.thisObject.callMethod("getVSlider") as SeekBar
                    val topValue = vToggleSliderInner.findViewByIdName("top_text") as TextView

                    topValue.visibility = View.VISIBLE
                    topValue.text = convertToPercentageProgress(seekBar.progress, seekBar.max)
                }

            // 控制中心二级亮度条进度值高级材质适配
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateBlendBlur")
                .first().createAfterHook {
                    val context = it.thisObject.callMethod("getContext") as Context
                    val vToggleSliderInner = it.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = vToggleSliderInner.findViewByIdName("top_text") as TextView

                    if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                        val color = vToggleSliderInner.resources.getColorBy("toggle_slider_top_text_color", plugin)
                        topValue.setTextColor(color)
                        miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                        miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                        return@createAfterHook
                    }
                    // Color.WHITE Color.parseColor("#959595")
                    topValue.setTextColor(Color.WHITE)
                    miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                    val colorArray = vToggleSliderInner.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
                }


            // 设置展开的大小
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateLargeSize")
                .first().createAfterHook {
                    val item = it.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = item.findViewByIdName("top_text") as TextView
                    topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                }

            // 设置未展开的大小
            brightnessPanelSliderController.methodFinder()
                .filterByName("updateSmallSize")
                .first().createAfterHook {
                    val item = it.thisObject.callMethod("getVToggleSliderInner") as ViewGroup
                    val topValue = item.findViewByIdName("top_text") as TextView
                    topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                }

            val brightnessPanelAnimator =
                loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelAnimator", classLoader)

            // 修复过渡动画错位，并增加大小过渡动画
            brightnessPanelAnimator.methodFinder()
                .filterByName("frameCallback")
                .first().createAfterHook {
                    val sliderController = it.thisObject.getObjectField("sliderController")
                    val item = callMethod(sliderController as String?, "getVToggleSliderInner") as ViewGroup
                    val topValue = item.findViewByIdName("top_text") as TextView
                    val icon = callMethod(sliderController, "getVIcon") as View

                    val sizeBgX = it.thisObject.getObjectField("sizeBgX") as Float
                    val left = (dpToPx(40f, topValue.resources.displayMetrics).toInt() - icon.layoutParams.width) / 2
                    topValue.left = icon.left - left
                    topValue.right = icon.right + left
                    topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f + 2f * sizeBgX)
                }

            // 修复展开动画错位之一
            brightnessPanelSliderController.constructorFinder()
                .toList().createAfterHooks {
                    val brightnessPanel = it.args[0] as FrameLayout

                    val topText = brightnessPanel.findViewByIdName("top_text") as TextView
                    topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    val mLayoutParams =
                        (topText.layoutParams as FrameLayout.LayoutParams).apply {
                            width = dpToPx(40f, topText.resources.displayMetrics).toInt()
                        }
                    topText.layoutParams = mLayoutParams
                }

            // 貌似用不到的修复展开动画错位的方法之一
            brightnessSliderController.methodFinder()
                .filterByName("createViewHolder")
                .filterByParamTypes {
                    it[0] == ViewGroup::class.java && it[1] == Int::class.java
                }.first().createAfterHook {
                    val viewHolder = it.result

                    if (viewHolder != null) {
                        val root = XposedHelpers.getObjectField(viewHolder, "itemView") as ViewGroup
                        val topText = root.findViewByIdName("top_text") as TextView
                        topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val mLayoutParams =
                            (topText.layoutParams as FrameLayout.LayoutParams).apply {
                                width = dpToPx(40f, root.resources.displayMetrics).toInt()
                            }
                        topText.layoutParams = mLayoutParams
                    }

                }

        }

        // 控制中心非展开状态下的进度值高级材质适配
        val toggleSliderViewHolder =
            loadClass("miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder", classLoader)

        toggleSliderViewHolder.methodFinder()
            .filterByName("updateBlendBlur")
            .first().createAfterHook {
                val context = it.thisObject.callMethod("getContext") as Context

                val item = it.thisObject.getObjectField("itemView") as View
                val topValue = item.findViewByIdName("top_text") as TextView

                if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                    val colorId = context.resources.getIdentifier("toggle_slider_top_text_color", "color", "miui.systemui.plugin")
                    val color = item.resources.getColor(colorId, null)

                    topValue.setTextColor(color)
                    miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                    miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                    return@createAfterHook
                }
                // Color.WHITE Color.parseColor("#959595")
                topValue.setTextColor(Color.WHITE)
                miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                val colorArray: IntArray =
                    context.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)

            }

    }

    private fun convertToPercentageProgress(
        progress: Int,
        max: Int
    ) = "${(progress * 100 / max)}%"


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
