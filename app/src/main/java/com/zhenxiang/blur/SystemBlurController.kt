package com.zhenxiang.blur

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap
import com.zhenxiang.blur.model.CornersRadius
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.S)
class SystemBlurController(
    private val view: View,
    backgroundColour: Int = if (mPrefsMap.getInt("blur_view_color", -1) != -1) mPrefsMap.getInt("blur_view_color", -1)
    else Color.parseColor("#44FFFFFF"),
    blurRadius: Int = mPrefsMap.getInt("home_blur_radius", 100),
    cornerRadius: CornersRadius = CornersRadius.all(0f),
) : View.OnAttachStateChangeListener {

    private var windowManager: WindowManager? = null
    private val crossWindowBlurListener = Consumer<Boolean> { blurEnabled = it }
    private var blurEnabled: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                updateBackgroundColour()
                updateBlurRadius()
            }
        }
    private var backgroundColour = backgroundColour
        set(value) {
            field = value
            updateBackgroundColour()
        }
    var blurRadius = blurRadius
        set(value) {
            field = value
            updateBlurRadius()
        }
    var cornerRadius = cornerRadius
        set(value) {
            field = value
            when (val bg = view.background) {
                is BackgroundBlurDrawable -> setCornerRadius(bg, value)
                is ShapeDrawable -> bg.shape = getShapeFromCorners(value)
            }
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On api 31 and above background init is done in onViewAttachedToWindow
            view.addOnAttachStateChangeListener(this)
        } else {
            // On pre api 31 init background here
            val shape = ShapeDrawable()
            shape.shape = getShapeFromCorners(cornerRadius)
            shape.paint.color = backgroundColour
            view.background = shape
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        windowManager = getWindowManager(view.context).apply {
            blurEnabled = isCrossWindowBlurEnabled
            addCrossWindowBlurEnabledListener(crossWindowBlurListener)
        }
        view.createBackgroundBlurDrawable()?.let {
            // Configure blur drawable with current values
            it.setColor(backgroundColour)
            it.setBlurRadius(blurRadius)
            setCornerRadius(it, cornerRadius)
            view.background = it
        }
    }

    override fun onViewDetachedFromWindow(_v: View) {
        // Clear blur drawable
        if (view.background is BackgroundBlurDrawable) {
            view.background = null
        }
        windowManager?.removeCrossWindowBlurEnabledListener(crossWindowBlurListener)
        windowManager = null
    }

    private fun updateBackgroundColour() {
        val bg = view.background
        when (bg) {
            is BackgroundBlurDrawable -> bg.setColor(backgroundColour)
            is ShapeDrawable -> bg.paint.color = backgroundColour
        }
        bg?.invalidateSelf()
    }

    private fun updateBlurRadius() {
        val bg = view.background
        if (bg is BackgroundBlurDrawable) {
            bg.setBlurRadius(if (blurEnabled) blurRadius else 0)
        }
    }

    private fun setCornerRadius(blurDrawable: BackgroundBlurDrawable, corners: CornersRadius) {
        blurDrawable.setCornerRadius(
            corners.topLeft, corners.topRight, corners.bottomLeft, corners.bottomRight
        )
    }

    private fun getShapeFromCorners(corners: CornersRadius): RoundRectShape {
        return RoundRectShape(getCornersFloatArray(corners), null, null)
    }

    private fun getCornersFloatArray(corners: CornersRadius): FloatArray {
        return floatArrayOf(
            corners.topLeft,
            corners.topLeft,
            corners.topRight,
            corners.topRight,
            corners.bottomRight,
            corners.bottomRight,
            corners.bottomLeft,
            corners.bottomLeft
        )
    }

    private fun getWindowManager(context: Context): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
}