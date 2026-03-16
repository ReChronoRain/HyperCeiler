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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.progress

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.SeekBar
import androidx.core.graphics.withClip
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.lerp
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.lerpInv
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.lerpInvSat
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.linearInterpolate
import com.sevtinge.hyperceiler.libhook.utils.api.dp
import kotlin.math.abs
import kotlin.math.cos

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/view/SquigglySeekBar.kt
@SuppressLint("AppCompatCustomView")
class SquigglySeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SeekBar(context, attrs, defStyleAttr) {
    companion object {
        val EMPHASIZED_DECELERATE: Interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
        val STANDARD_DECELERATE: Interpolator = PathInterpolator(0f, 0f, 0f, 1f)
        val TOUCH_SPRING = SpringInterpolator(0.95f, 0.35f)

        const val TWO_PI = (Math.PI * 2f).toFloat()

        private const val HEIGHT_DP = 16

        private const val ALPHA = 0xFF
        private const val DISABLED_ALPHA = 0x4D

        private const val THUMB_NORMAL_HEIGHT_DP = 10
        private const val THUMB_PRESSED_HEIGHT_DP = 16
        private const val THUMB_V_BAR_WIDTH_DP = 4
        private const val THUMB_V_BAR_HEIGHT_DP = 14
    }

    private val thumbHeight: Int = THUMB_NORMAL_HEIGHT_DP.dp(context)
    private val thumbHeightPressed: Int = THUMB_PRESSED_HEIGHT_DP.dp(context)
    private val thumbVBarWidth: Int = THUMB_V_BAR_WIDTH_DP.dp(context)
    private val thumbVBarHeight: Int = THUMB_V_BAR_HEIGHT_DP.dp(context)

    private val wavePaint = Paint()
    private val linePaint = Paint()
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val path = Path()
    private var heightFraction = 0f
    private var heightAnimator: ValueAnimator? = null
    private var phaseOffset = 0f
    private var lastFrameTime = -1L

    private var baseColor: Int = Color.WHITE
    private var touchAnimProgress = 0f // 0f..1f
    private var touchAnimator: ValueAnimator? = null

    /* distance over which amplitude drops to zero, measured in wavelengths */
    private val transitionPeriods = 1.5f

    /* wave endpoint as percentage of bar when play position is zero */
    private val minWaveEndpoint = 0.2f

    /* wave endpoint as percentage of bar when play position matches wave endpoint */
    private val matchedWaveEndpoint = 0.6f

    // Horizontal length of the sine wave
    var waveLength = 0f

    // Height of each peak of the sine wave
    var lineAmplitude = 0f

    // Line speed in px per second
    var phaseSpeed = 0f

    // Progress stroke width, both for wave and solid line
    var strokeWidth = 0f
        set(value) {
            if (field == value) {
                return
            }
            field = value
            wavePaint.strokeWidth = value
            linePaint.strokeWidth = value
        }

    // Enables a transition region where the amplitude
    // of the wave is reduced linearly across it.
    var transitionEnabled = true
        set(value) {
            field = value
            invalidate()
        }

    var animate: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (field) {
                lastFrameTime = SystemClock.uptimeMillis()
            }
            heightAnimator?.cancel()
            heightAnimator =
                ValueAnimator.ofFloat(heightFraction, if (animate) 1f else 0f).apply {
                    if (animate) {
                        startDelay = 60
                        duration = 800
                        interpolator = EMPHASIZED_DECELERATE
                    } else {
                        duration = 550
                        interpolator = STANDARD_DECELERATE
                    }
                    addUpdateListener {
                        heightFraction = it.animatedValue as Float
                        invalidate()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                heightAnimator = null
                            }
                        }
                    )
                    start()
                }
        }

    init {
        wavePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.style = Paint.Style.STROKE
        wavePaint.style = Paint.Style.STROKE
        linePaint.alpha = DISABLED_ALPHA
        updateColorsFromTint()
    }

    var thumbStyle: ThumbStyle = ThumbStyle.Hidden
        set(value) {
            field = value
            adjustThumb()
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredHeight = HEIGHT_DP.dp(context) + paddingTop + paddingBottom
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (animate) {
            invalidate()
            val now = SystemClock.uptimeMillis()
            phaseOffset += (now - lastFrameTime) / 1000f * phaseSpeed
            phaseOffset %= waveLength
            lastFrameTime = now
        }

        val progress = progress.toFloat() / max.toFloat()
        val centerY = height / 2.0f
        val totalWidth = width.toFloat()
        val totalProgressPx = totalWidth * progress
        val waveProgressPx =
            totalWidth *
                (if (!transitionEnabled || progress > matchedWaveEndpoint) progress
                else
                    lerp(
                        minWaveEndpoint,
                        matchedWaveEndpoint,
                        lerpInv(0f, matchedWaveEndpoint, progress)
                    ))
        // Build Wiggly Path
        val waveStart = -phaseOffset - waveLength / 2f
        val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx
        // helper function, computes amplitude for wave segment
//        val computeAmplitude: (Float, Float) -> Float = { x, sign ->
//            if (transitionEnabled) {
//                val length = transitionPeriods * waveLength
//                val coeff =
//                    lerpInvSat(waveProgressPx + length / 2f, waveProgressPx - length / 2f, x)
//                sign * heightFraction * lineAmplitude * coeff
//            } else {
//                sign * heightFraction * lineAmplitude
//            }
//        }
        // Reset path object to the start
        path.rewind()
        path.moveTo(waveStart, 0f)
        // Build the wave, incrementing by half the wavelength each time
        var currentX = waveStart
        var waveSign = 1f
        var currentAmp = computeAmplitude(currentX, waveSign, waveProgressPx)
        val dist = waveLength / 2f
        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2
            val nextAmp = computeAmplitude(nextX, waveSign, waveProgressPx)
            path.cubicTo(midX, currentAmp, midX, nextAmp, nextX, nextAmp)
            currentAmp = nextAmp
            currentX = nextX
        }
        // translate to the start position of the progress bar for all draw commands
        val clipTop = lineAmplitude + strokeWidth
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), centerY)
        // Draw path up to progress position
        canvas.save()
        canvas.clipRect(0f, -1f * clipTop, totalProgressPx, clipTop)
        canvas.drawPath(path, wavePaint)
        canvas.restore()
        if (transitionEnabled) {
            // If there's a smooth transition, we draw the rest of the
            // path in a different color (using different clip params)
            canvas.withClip(totalProgressPx, -1f * clipTop, totalWidth, clipTop) {
                drawPath(path, linePaint)
            }
        } else {
            // No transition, just draw a flat line to the end of the region.
            // The discontinuity is hidden by the progress bar thumb shape.
            canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        }
        // Draw round line cap at the beginning of the wave
        val startAmp = cos(abs(waveStart) / waveLength * TWO_PI)
        canvas.drawPoint(0f, startAmp * lineAmplitude * heightFraction, wavePaint)
        canvas.restore()

        // Draw thumb
        val thumbCenterX = paddingLeft + totalProgressPx
        when (thumbStyle) {
            ThumbStyle.Circle -> {
                val currentThumbHeight = linearInterpolate(thumbHeight, thumbHeightPressed, touchAnimProgress).coerceAtLeast(strokeWidth.toInt())
                canvas.drawCircle(thumbCenterX, centerY, currentThumbHeight / 2.0f, thumbPaint)
            }
            ThumbStyle.VerticalBar -> {
                val halfWidth = thumbVBarWidth / 2.0f
                val halfHeight = thumbVBarHeight / 2.0f
                canvas.drawRoundRect(thumbCenterX - halfWidth, centerY - halfHeight, thumbCenterX + halfWidth, centerY + halfHeight, halfWidth, halfWidth, thumbPaint)
            }
            else -> {}
        }

//        if (animate) {
//            invalidate()
//            val now = SystemClock.uptimeMillis()
//            phaseOffset += (now - lastFrameTime) / 1000f * phaseSpeed
//            phaseOffset %= waveLength
//            lastFrameTime = now
//        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val superResult = super.onTouchEvent(event)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startAnimation(1f) // 按下变大
                // 解决 ScrollView 冲突
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startAnimation(0f) // 松手恢复
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return superResult
    }

    override fun setProgressTintList(tint: ColorStateList?) {
        super.setProgressTintList(tint)
        updateColorsFromTint()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        updateColorsFromTint()
    }

    private fun adjustThumb() {
        val requiredPadding = when (thumbStyle) {
            ThumbStyle.Circle -> thumbHeightPressed / 2
            ThumbStyle.VerticalBar -> thumbVBarWidth / 2
            else -> 0
        }
        setPadding(requiredPadding, paddingTop, requiredPadding, paddingBottom)
    }

    private fun computeAmplitude(x: Float, sign: Float, waveProgressPx: Float): Float {
        return if (transitionEnabled) {
            val length = transitionPeriods * waveLength
            val coeff = lerpInvSat(waveProgressPx + length / 2f, waveProgressPx - length / 2f, x)
            sign * heightFraction * lineAmplitude * coeff
        } else {
            sign * heightFraction * lineAmplitude
        }
    }

    private fun startAnimation(target: Float) {
        if (touchAnimProgress == target) return
        touchAnimator?.cancel()
        touchAnimator = ValueAnimator.ofFloat(touchAnimProgress, target).apply {
            duration = 200
            interpolator = TOUCH_SPRING
            addUpdateListener {
                touchAnimProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        touchAnimator = null
                    }
                }
            )
            start()
        }
    }

    private fun updateColorsFromTint() {
        progressTintList?.getColorForState(drawableState, baseColor)?.let {
            baseColor = it
        }
        val rgb = baseColor and 0x00FFFFFF
        wavePaint.color = rgb or (ALPHA shl 24)
        thumbPaint.color = rgb or (ALPHA shl 24)
        linePaint.color = rgb or (DISABLED_ALPHA shl 24)
        invalidate()
    }
}
