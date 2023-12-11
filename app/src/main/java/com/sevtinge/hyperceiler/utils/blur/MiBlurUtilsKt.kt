package com.sevtinge.hyperceiler.utils.blur

import android.graphics.Outline
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import com.sevtinge.hyperceiler.utils.devicesdk.isDarkMode
import org.lsposed.hiddenapibypass.HiddenApiBypass

object MiBlurUtilsKt {
    var mSupportedMiBlur: Boolean = true

    fun setViewBackgroundBlur(view: View?, i: Int) {
        HiddenApiBypass.invoke(View::class.java, view, "setMiBackgroundBlurMode", i)
    }

    fun setViewBlur(view: View, i: Int) {
        HiddenApiBypass.invoke(View::class.java, view, "setMiViewBlurMode", i)
    }

    fun setBlurRadius(view: View, i: Int) {
        if (i < 0 || i > 200) {
            Log.e("MiBlurUtilsKt", "setMiBackgroundBlurRadius error radius is " + i + " " + view.javaClass.getName() + " hashcode " + view.hashCode())
            return
        }
        HiddenApiBypass.invoke(View::class.java, view, "setMiBackgroundBlurRadius", i)
    }

    fun setPassWindowBlurEnable(view: View, z: Boolean) {
        Log.d("Launcher.BlurUtilities", "setViewBlur:  view $view")
        HiddenApiBypass.invoke(View::class.java, view, "setPassWindowBlurEnabled", z)
    }

    fun disableMiBackgroundContainBelow(view: View, z: Boolean) {
        HiddenApiBypass.invoke(View::class.java, view, "disableMiBackgroundContainBelow", z)
    }

    fun setBlurColor(view: View, i: Int, i2: Int) {
        HiddenApiBypass.invoke(View::class.java, view, "addMiBackgroundBlendColor", i, i2)
    }

    fun resetBlurColor(view: View) {
        HiddenApiBypass.invoke(View::class.java, view, "clearMiBackgroundBlendColor")
    }

    fun setBlurRoundRect(view: View, i: Int, i2: Int, i3: Int, i4: Int, i5: Int) {
        view.setClipToOutline(false)
        val outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    i2, i3, i4, i5, i.toFloat()
                )
            }
        }
        view.outlineProvider = outlineProvider
    }

    fun setBlurRoundRect(view: View, i: Int) {
        view.setClipToOutline(true)
        val outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0, view.width, view.height, i.toFloat()
                )
            }
        }
        view.outlineProvider = outlineProvider
    }


    fun clearAllBlur(view: View) {
        resetBlurColor(view)
        setViewBackgroundBlur(view, 0)
        setViewBlur(view, 0)
        setBlurRadius(view, 0)
        setPassWindowBlurEnable(view, false)
    }

    fun setElementBlur(view: View, i: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int) {
        resetBlurColor(view)
        setViewBlur(view, i)
        setBlurRoundRect(view, i2)
        if (isDarkMode()) {
            setBlurColor(view, i3, i4)
            setBlurColor(view, i7, i8)
            return
        }
        setBlurColor(view, i5, i6)
        setBlurColor(view, i9, i10)
    }

    fun setContainerBlur(view: View, i: Int, z: Boolean, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int) {
        resetBlurColor(view)
        setViewBackgroundBlur(view, i)
        if (z) {
            setPassWindowBlurEnable(view, true)
        }
        setBlurRadius(view, i2)
        if (isDarkMode()) {
            setBlurColor(view, i3, i4)
        } else {
            setBlurColor(view, i5, i6)
        }
    }
}
