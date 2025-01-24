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

    fun View.setPassWindowBlurEnabled(z: Boolean) {
        setPassWindowBlurEnabled.invoke(this, z)
    }

    fun View.disableMiBackgroundContainBelow(z: Boolean) {
        disableMiBackgroundContainBelow.invoke(this, z)
    }

    fun View.addMiBackgroundBlendColor(i: Int, i2: Int) {
        addMiBackgroundBlendColor(this, i, i2)
    }

    fun View.clearMiBackgroundBlendColor() {
        clearMiBackgroundBlendColor.invoke(this)
    }

    fun View.setBackgroundBlurScaleRatio(ratio: Float) {
        setMiBackgroundBlurScaleRatio.invoke(this, ratio)
    }

    fun View.setMiBackgroundBlendColors(iArr: IntArray, f: Float) {
        var z: Boolean
        this.clearMiBackgroundBlendColor()
        val length = iArr.size / 2
        for (i in 0 until length) {
            val i2 = i * 2
            var i3 = iArr[i2]
            z = f == 1.0f
            if (!z) {
                val i4 = (i3 shr 24) and 255
                i3 = (i3 and ((i4 shl 24).inv())) or (((i4 * f).toInt()) shl 24)
            }
            val i5 = iArr[i2 + 1]
            this.addMiBackgroundBlendColor(i3, i5)
        }
    }

    fun View.setBlurRoundRect(i: Int, i2: Int, i3: Int, i4: Int, i5: Int) {
        this.clipToOutline = false
        val outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    i2, i3, i4, i5, i.toFloat()
                )
            }
        }
        this.outlineProvider = outlineProvider
    }

    fun View.setBlurRoundRect(i: Int) {
        this.clipToOutline = true
        val outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0, view.width, view.height, i.toFloat()
                )
            }
        }
        this.outlineProvider = outlineProvider
    }

    fun View.clearAllBlur() {
        clearMiBackgroundBlendColor()
        setMiBackgroundBlurMode(0)
        setMiViewBlurMode(0)
        setMiBackgroundBlurRadius(0)
        setPassWindowBlurEnabled(false)
    }
}
