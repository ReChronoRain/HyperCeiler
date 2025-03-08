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
package com.sevtinge.hyperceiler.utils.blur

import android.graphics.*
import android.view.*

object MiBlurUtilsKt {

    private val setMiViewBlurMode by lazy {
        View::class.java.getDeclaredMethod("setMiViewBlurMode", Integer.TYPE)
    }

    private val setMiBackgroundBlurMode by lazy {
        View::class.java.getDeclaredMethod("setMiBackgroundBlurMode", Integer.TYPE)
    }

    private val setPassWindowBlurEnabled by lazy {
        View::class.java.getDeclaredMethod("setPassWindowBlurEnabled", java.lang.Boolean.TYPE)
    }

    private val setMiBackgroundBlurRadius by lazy {
        View::class.java.getDeclaredMethod("setMiBackgroundBlurRadius", Integer.TYPE)
    }

    private val addMiBackgroundBlendColor by lazy {
        View::class.java.getDeclaredMethod("addMiBackgroundBlendColor", Integer.TYPE, Integer.TYPE)
    }

    private val setMiBackgroundBlurScaleRatio by lazy {
        View::class.java.getDeclaredMethod("setMiBackgroundBlurScaleRatio", java.lang.Float.TYPE)
    }

    private val clearMiBackgroundBlendColor by lazy {
        View::class.java.getDeclaredMethod("clearMiBackgroundBlendColor")
    }

    private val disableMiBackgroundContainBelow by lazy {
        View::class.java.getDeclaredMethod("disableMiBackgroundContainBelow", java.lang.Boolean.TYPE)
    }

    fun View.setMiBackgroundBlurMode(mode: Int) {
        setMiBackgroundBlurMode.invoke(this, mode)
    }

    fun View.setMiViewBlurMode(mode: Int) {
        setMiViewBlurMode.invoke(this, mode)
    }

    fun View.setMiBackgroundBlurRadius(radius: Int) {
        /*if (radius < 0 || radius > 200) {
            Log.e("MiBlurUtils", "setMiBackgroundBlurRadius error radius is " + radius + " " + this.javaClass.getName() + " hashcode " + this.hashCode())
            return
        }*/
        setMiBackgroundBlurRadius.invoke(this, radius)
    }

    fun View.setPassWindowBlurEnabled(isEnabled: Boolean) {
        setPassWindowBlurEnabled.invoke(this, isEnabled)
    }

    fun View.disableMiBackgroundContainBelow(isEnabled: Boolean) {
        disableMiBackgroundContainBelow.invoke(this, isEnabled)
    }

    fun View.addMiBackgroundBlendColor(color: Int, mode: Int) {
        addMiBackgroundBlendColor(this, color, mode)
    }

    fun View.clearMiBackgroundBlendColor() {
        clearMiBackgroundBlendColor.invoke(this)
    }

    fun View.setBackgroundBlurScaleRatio(ratio: Float) {
        setMiBackgroundBlurScaleRatio.invoke(this, ratio)
    }

    fun View.setMiBackgroundBlendColors(colors: IntArray, ratio: Float) {
        clearMiBackgroundBlendColor()
        for (i in 0 until colors.size / 2) {
            val j = i * 2
            var color = colors[j]
            if (ratio != 1.0f) {
                val alpha = (color shr 24) and 255
                color = (color and (alpha shl 24).inv()) or ((alpha * ratio).toInt() shl 24)
            }
            addMiBackgroundBlendColor(color, colors[j + 1])
        }
    }

    fun View.setBlurRoundRect(radius: Int, left: Int, top: Int, right: Int, bottom: Int) {
        clipToOutline = false
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(left, top, right, bottom, radius.toFloat())
            }
        }
    }

    fun View.setBlurRoundRect(radius: Int) {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
            }
        }
    }

    fun View.clearAllBlur() {
        clearMiBackgroundBlendColor()
        setMiBackgroundBlurMode(0)
        setMiViewBlurMode(0)
        setMiBackgroundBlurRadius(0)
        setPassWindowBlurEnabled(false)
    }
}
