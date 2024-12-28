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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap

import android.content.*
import android.graphics.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import de.robv.android.xposed.*
import java.lang.reflect.*

// from https://github.com/YunZiA/HyperStar2.0/blob/master/app/src/main/java/com/yunzia/hyperstar/hook/app/plugin/QSVolumeOrBrightnessValue.kt
object QSVolumeOrBrightnessValue {

    private val volumeShow = mPrefsMap.getBoolean("system_ui_control_center_qs_brightness_top_value_show")
    val volumeShowStyle = mPrefsMap.getStringAsInt("system_ui_control_center_qs_brightness_top_value_show_value", 0)
    private val brightnessShow = mPrefsMap.getBoolean("system_ui_control_center_qs_volume_top_value_show")
    val brightnessShowStyle = mPrefsMap.getStringAsInt("system_ui_control_center_qs_volume_top_value_show_value", 0)

    fun initQSVolumeOrBrightnessValue(classLoader: ClassLoader) {
        if (volumeShow){
            val VolumeSliderController = XposedHelpers.findClass("miui.systemui.controlcenter.panel.main.volume.VolumeSliderController",classLoader)
            XposedHelpers.findAndHookMethod(VolumeSliderController,"updateIconProgress",
                Boolean::class.java,object : XC_MethodHook(){
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        val thisObj = param?.thisObject
                        val str: String
                        val sliderHolder = XposedHelpers.callMethod(thisObj,"getHolder") ?: return
                        val item = XposedHelpers.getObjectField(sliderHolder,"itemView") as View

                        val seekBar = item.findViewByIdName("slider") as SeekBar
                        val max = seekBar.max
                        val value: Int = XposedHelpers.callMethod(thisObj,"getTargetValue") as Int
                        val topValue = item.findViewByIdName("top_text") as TextView
                        str = if (volumeShowStyle == 0) ((value * 100) / max).toString() + "%" else value.toString()

                        topValue.visibility = View.VISIBLE
                        topValue.text = str

                    }
                })
        }

        if (brightnessShow){
            val BrightnessSliderController = XposedHelpers.findClass("miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController",classLoader)

            XposedHelpers.findAndHookMethod(BrightnessSliderController,"updateIconProgress",object  : XC_MethodHook(){
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val thisObj = param?.thisObject
                    val str: String
                    val sliderHolder = XposedHelpers.callMethod(thisObj,"getSliderHolder")
                    if (sliderHolder == null){
                        return
                    }
                    val item = XposedHelpers.getObjectField(sliderHolder,"itemView") as View

                    val seekBar = XposedHelpers.callMethod(thisObj,"getSlider") as SeekBar
                    val max = seekBar.max
                    val value: Int = seekBar.progress
                    val topValue = item.findViewByIdName("top_text") as TextView
                    str = if (brightnessShowStyle == 0) ((value * 100) / max).toString() + "%" else value.toString()

                    topValue.visibility = View.VISIBLE
                    topValue.text = str


                }
            })
        }

        if (!brightnessShow && !volumeShow){
            return
        }

        val ToggleSliderViewHolder = XposedHelpers.findClass("miui.systemui.controlcenter.panel.main.recyclerview.ToggleSliderViewHolder",classLoader)
        val ControlCenterUtils = XposedHelpers.findClass("miui.systemui.controlcenter.utils.ControlCenterUtils",classLoader)
        val MiBlurCompat = XposedHelpers.findClass("miui.systemui.util.MiBlurCompat",classLoader)
        var colorArray : IntArray? = null

        XposedHelpers.findAndHookMethod(ToggleSliderViewHolder,"updateBlendBlur",object : XC_MethodHook(){

            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val thisObj = param?.thisObject

                val context = XposedHelpers.callMethod(thisObj,"getContext") as Context

                val default = XposedHelpers.callStaticMethod(ControlCenterUtils,"getBackgroundBlurOpenedInDefaultTheme",context) as Boolean

                val item = XposedHelpers.getObjectField(thisObj,"itemView") as View
                val topValue = item.findViewByIdName("top_text") as TextView
                val icon = item.findViewByIdName("icon")

                if (!default){
                    val colorId = context.resources.getIdentifier("toggle_slider_top_text_color", "color", "miui.systemui.plugin")
                    val color = item.resources.getColor(colorId)

                    topValue.setTextColor(color)
                    XposedHelpers.callStaticMethod(MiBlurCompat,"setMiViewBlurModeCompat",topValue,0)
                    XposedHelpers.callStaticMethod(MiBlurCompat,"clearMiBackgroundBlendColorCompat",topValue)

                    return
                }
                // Color.WHITE Color.parseColor("#959595")
                topValue.setTextColor(Color.WHITE)
                XposedHelpers.callStaticMethod(MiBlurCompat,"setMiViewBlurModeCompat",topValue,3)
                if (colorArray == null){
                    val array = context.resources.getIdentifier("toggle_slider_icon_blend_colors", "array", "miui.systemui.plugin")

                    colorArray = item.resources.getIntArray(array)
                }

                // val setMiProgressIconBackgroundBlendColors = XposedHelpers.findMethodBestMatch(MiBlurCompat,"setMiProgressIconBackgroundBlendColors",View::class.java,Int::class.java,Float::class.java)
                val method: Method = MiBlurCompat.getDeclaredMethod(
                    "setMiBackgroundBlendColors",
                    View::class.java,
                    IntArray::class.java,
                    Float::class.java
                )

                val iconColorArray : IntArray = colorArray as IntArray

                method.invoke(thisObj,icon,iconColorArray,1f)

                val valueColorArray : IntArray = colorArray as IntArray

                method.invoke(thisObj,topValue,valueColorArray,1f)

            }
        })
    }
}