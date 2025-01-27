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

package com.sevtinge.hyperceiler.module.hook.systemui.plugin

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
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.sevtinge.hyperceiler.module.base.tool.HookTool
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
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
            findAndHookMethod(
                volumeSliderController, "updateIconProgress", Boolean::class.java,
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject

                        val sliderHolder = callMethod(thisObj, "getHolder") ?: return
                        val item = getObjectField(sliderHolder, "itemView") as View
                        val topValue = item.findViewByIdName("top_text") as TextView
                        val sliderMaxValue = getObjectField(thisObj, "sliderMaxValue") as Int
                        val value = callMethod(thisObj, "getTargetValue")!! as Int
                        val level = callStaticMethod(volumeUtils, "progressToLevel", sliderMaxValue, value) as Int

                        topValue.visibility = View.VISIBLE
                        topValue.text = convertToPercentageProgress(level, sliderMaxValue / 1000)

                    }

                })

            val volumePanelViewController =
                loadClass("com.android.systemui.miui.volume.VolumePanelViewController", classLoader)
            val util = Util(classLoader)

            // 侧边音量条进度值 && All 场景二级音量条进度值 UI 启用
            findAndHookMethod(
                volumePanelViewController, "updateSuperVolumeView",
                "com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val mExpanded = getObjectField(thisObj, "mExpanded") as Boolean
                        val volumeColumn = param.args[0]
                        val superVolume = getObjectField(volumeColumn, "superVolume") as TextView
                        val mSuperVolumeBg = getObjectField(thisObj, "mSuperVolumeBg") as View

                        util.setVisOrGone(mSuperVolumeBg, !mExpanded) // 侧边音量条进度值 UI 显示
                        util.setVisOrGone(superVolume, mExpanded) // All 场景二级音量条进度值 UI 显示

                    }
                })


            // All 场景二级音量条百分比进度值计算显示
            findAndHookMethod(
                volumePanelViewController, "updateVolumeColumnSliderH",
                "com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn",
                Boolean::class.java, Int::class.java, Boolean::class.java, Int::class.java,
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val volumeColumn = param.args[0]

                        val mState = getObjectField(thisObj, "mState")
                        val states = getObjectField(mState, "states") as SparseArray<*>
                        val stream = getObjectField(volumeColumn, "stream") as Int
                        val streamState = states.get(getObjectField(volumeColumn, "stream") as Int)
                        val mActiveStream = getObjectField(thisObj, "mActiveStream") as Int

                        if (streamState != null) {

                            val maxLevel = getObjectField(streamState, "levelMax") as Int
                            val level = getObjectField(streamState, "level") as Int

                            (getObjectField(volumeColumn, "superVolume") as TextView).text =
                                convertToPercentageProgress(level, maxLevel)

                            if (stream == mActiveStream) {
                                (getObjectField(thisObj, "mSuperVolume") as TextView).text =
                                    convertToPercentageProgress(level, maxLevel)

                            }

                        }

                    }
                })

            // 为 All 二级进度值适配高级材质
            findAndHookMethod(
                volumePanelViewController, "updateColumnSliderBlendColor",
                "com.android.systemui.miui.volume.VolumePanelViewController\$VolumeColumn",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val volumeColumn = param.args[0]
                        val mContext = getObjectField(thisObj, "mContext") as Context
                        val mExpanded = getObjectField(thisObj, "mExpanded") as Boolean
                        val mNeedShowDialog = getObjectField(thisObj, "mNeedShowDialog") as Boolean
                        val colorArrayName = if (!mExpanded) {
                            "miui_expanded_button_and_seekbar_icon_blend_colors_collapsed"
                        } else if (mNeedShowDialog) {
                            "miui_seekbar_icon_blend_colors_expanded"
                        } else {
                            "miui_seekbar_icon_blend_colors_expanded_cc"
                        }
                        val superVolume = getObjectField(volumeColumn, "superVolume") as TextView
                        superVolume.setTextColor(Color.WHITE)
                        val colorArray = mContext.resources.getIntArrayBy(colorArrayName, plugin)
                        util.setMiViewBlurAndBlendColor(superVolume, mExpanded, mContext, 3, colorArray, false)

                    }
                })

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
            findAndHookMethod(
                brightnessSliderController, "updateIconProgress",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val sliderHolder = callMethod(thisObj, "getSliderHolder") ?: return

                        val item = getObjectField(sliderHolder, "itemView") as View
                        val seekBar = callMethod(thisObj, "getSlider") as SeekBar
                        val topValue = item.findViewByIdName("top_text") as TextView

                        topValue.visibility = View.VISIBLE
                        topValue.text = convertToPercentageProgress(seekBar.progress, seekBar.max)


                    }
                })

            // 控制中心二级亮度条百分比进度值计算
            findAndHookMethod(
                brightnessPanelSliderController, "updateIconProgress",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val vToggleSliderInner =
                            callMethod(thisObj, "getVToggleSliderInner") as ViewGroup
                        val seekBar = callMethod(thisObj, "getVSlider") as SeekBar
                        val topValue = vToggleSliderInner.findViewByIdName("top_text") as TextView

                        topValue.visibility = View.VISIBLE
                        topValue.text = convertToPercentageProgress(seekBar.progress, seekBar.max)
                    }

                })

            // 控制中心二级亮度条进度值高级材质适配
            findAndHookMethod(
                brightnessPanelSliderController, "updateBlendBlur",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val context = callMethod(thisObj, "getContext") as Context
                        val vToggleSliderInner =
                            callMethod(thisObj, "getVToggleSliderInner") as ViewGroup
                        val topValue = vToggleSliderInner.findViewByIdName("top_text") as TextView

                        if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                            val color =
                                vToggleSliderInner.resources.getColorBy("toggle_slider_top_text_color", plugin)
                            topValue.setTextColor(color)
                            miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                            miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)

                            return
                        }
                        // Color.WHITE Color.parseColor("#959595")
                        topValue.setTextColor(Color.WHITE)
                        miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                        val colorArray =
                            vToggleSliderInner.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                        miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)
                    }

                })


            // 设置展开的大小
            findAndHookMethod(
                brightnessPanelSliderController, "updateLargeSize",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val item = callMethod(thisObj, "getVToggleSliderInner") as ViewGroup
                        val topValue = item.findViewByIdName("top_text") as TextView
                        topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                    }
                })

            // 设置未展开的大小
            findAndHookMethod(
                brightnessPanelSliderController, "updateSmallSize",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val item = callMethod(thisObj, "getVToggleSliderInner") as ViewGroup
                        val topValue = item.findViewByIdName("top_text") as TextView
                        topValue.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)

                    }
                })

            val brightnessPanelAnimator =
                loadClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelAnimator", classLoader)

            // 修复过渡动画错位，并增加大小过渡动画
            findAndHookMethod(
                brightnessPanelAnimator, "frameCallback",
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val thisObj = param.thisObject
                        val sliderController = getObjectField(thisObj, "sliderController")
                        val item =
                            callMethod(sliderController, "getVToggleSliderInner") as ViewGroup
                        val topValue = item.findViewByIdName("top_text") as TextView
                        val icon = callMethod(sliderController, "getVIcon") as View

                        val sizeBgX = getObjectField(thisObj, "sizeBgX") as Float
                        val left = (dpToPx(40f, topValue.resources.displayMetrics).toInt() - icon.layoutParams.width) / 2
                        topValue.left = icon.left - left
                        topValue.right = icon.right + left
                        topValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f + 2f * sizeBgX)
                    }
                })

            // 修复展开动画错位之一
            XposedBridge.hookAllConstructors(brightnessPanelSliderController,
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val brightnessPanel = param.args[0] as FrameLayout

                        val topText = brightnessPanel.findViewByIdName("top_text") as TextView
                        topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        val mLayoutParams =
                            (topText.layoutParams as FrameLayout.LayoutParams).apply {
                                width = dpToPx(40f, topText.resources.displayMetrics).toInt()
                            }
                        topText.layoutParams = mLayoutParams

                    }
                })

            // 貌似用不到的修复展开动画错位的方法之一
            findAndHookMethod(
                brightnessSliderController, "createViewHolder",
                ViewGroup::class.java, Int::class.java,
                object : HookTool.MethodHook() {
                    override fun after(param: MethodHookParam) {
                        val viewHolder = param.result

                        if (viewHolder != null) {
                            val root = getObjectField(viewHolder, "itemView") as ViewGroup
                            val topText = root.findViewByIdName("top_text") as TextView
                            topText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            val mLayoutParams =
                                (topText.layoutParams as FrameLayout.LayoutParams).apply {
                                    width = dpToPx(40f, root.resources.displayMetrics).toInt()
                                }
                            topText.layoutParams = mLayoutParams
                        }

                    }
                })

        }

        // 控制中心非展开状态下的进度值高级材质适配
        val toggleSliderViewHolder =
            loadClass("miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder", classLoader)

        findAndHookMethod(
            toggleSliderViewHolder, "updateBlendBlur",
            object : HookTool.MethodHook() {
                override fun after(param: MethodHookParam) {
                    val thisObj = param.thisObject

                    val context = callMethod(thisObj, "getContext") as Context

                    val item = getObjectField(thisObj, "itemView") as View
                    val topValue = item.findViewByIdName("top_text") as TextView

                    if (!controlCenterUtils.getBackgroundBlurOpenedInDefaultTheme(context)) {
                        val colorId = context.resources.getIdentifier("toggle_slider_top_text_color", "color", "miui.systemui.plugin")
                        val color = item.resources.getColor(colorId, null)

                        topValue.setTextColor(color)
                        miBlurCompat.setMiViewBlurModeCompat(topValue, 0)
                        miBlurCompat.clearMiBackgroundBlendColorCompat(topValue)
                        return
                    }
                    // Color.WHITE Color.parseColor("#959595")
                    topValue.setTextColor(Color.WHITE)
                    miBlurCompat.setMiViewBlurModeCompat(topValue, 3)

                    val colorArray: IntArray =
                        context.resources.getIntArrayBy("toggle_slider_icon_blend_colors", plugin)
                    miBlurCompat.setMiBackgroundBlendColors(topValue, colorArray, 1f)

                }
            })

    }

    private fun convertToPercentageProgress(
        progress: Int,
        max: Int
    ) = "${(progress * 100 / max)}%"


    class ControlCenterUtils(classLoader: ClassLoader?) {

        private val controlCenterUtils =
            loadClass("miui.systemui.controlcenter.utils.ControlCenterUtils", classLoader)

        fun getBackgroundBlurOpenedInDefaultTheme(context: Context) =
            callStaticMethod(
                controlCenterUtils, "getBackgroundBlurOpenedInDefaultTheme", context
            ) as Boolean
    }

    class MiBlurCompat(classLoader: ClassLoader?) {
        private val miBlurCompat = loadClass("miui.systemui.util.MiBlurCompat", classLoader)

        fun setMiViewBlurModeCompat(view: View, mode: Int) {
            callStaticMethod(miBlurCompat, "setMiViewBlurModeCompat", view, mode)
        }

        fun clearMiBackgroundBlendColorCompat(view: View) {
            callStaticMethod(miBlurCompat, "clearMiBackgroundBlendColorCompat", view)
        }

        fun setMiBackgroundBlendColors(view: View?, colorArray: IntArray, float: Float) {
            callStaticMethod(miBlurCompat, "setMiBackgroundBlendColors", view, colorArray, float)
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
            callStaticMethod(util, "setMiViewBlurAndBlendColor", view, z, context, i, iArr, z2)
        }

        fun setVisOrGone(
            view: View,
            visOrGone: Boolean
        ) {
            callStaticMethod(util, "setVisOrGone", view, visOrGone)
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
