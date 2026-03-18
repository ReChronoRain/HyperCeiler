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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils.linearInterpolate

// https://github.com/HowieHChen/XiaomiHelper/blob/6a0e424ad9276205fdf47f523cc6c8bb72e49e7f/app/src/main/kotlin/dev/lackluster/mihelper/hook/drawable/AmbientLightDrawable.kt
class AmbientLightDrawable(
    private val useAnim: Boolean = true
) : Drawable() {
    private var runtimeShader: RuntimeShader? = null
    private val paint = Paint()
    private val gradientPositions = floatArrayOf(0.9f, -0.21f, 0.1f, -0.26f, 0.5f, -0.28f)
    private val colorVec4 = floatArrayOf(0f, 0f, 0f, 1f)

    private var pause = false
    private var startTime = SystemClock.elapsedRealtime()
    private var pauseTime = 0L
    private var pauseDuration = 0L

    private var colorState: AnimationState = AnimationState.DONE
    private var colorStartTimeMillis = 0L
    private var sourceColor = Color.TRANSPARENT
    private var currentColor = Color.TRANSPARENT
    private var targetColor = Color.TRANSPARENT

    private var resizeState: AnimationState = AnimationState.DONE
    private var resizeStartTimeMillis = 0L
    private var sourceHeight = 0
    private var currentHeight = 0
    private var targetHeight = 0

    private var isLightMode = 0
    private var nextResizeAnim = false

    init {
        try {
            runtimeShader = RuntimeShader(AGSL_SRC)
            paint.shader = runtimeShader
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun animateNextResize() {
        nextResizeAnim = true
    }

    fun setLightMode(light: Boolean) {
        isLightMode = if (light) 1 else 0
    }

    fun setGradientColor(color: Int, skipAnim: Boolean = false) {
        if (targetColor == color) return

        val hostView = callback as? View
        val shouldSnap = !useAnim || skipAnim || hostView == null || !hostView.isShown || !hostView.isAttachedToWindow

        if (shouldSnap) {
            sourceColor = color
            currentColor = color
            colorState = AnimationState.DONE
        } else {
            sourceColor = currentColor
            targetColor = color
            colorState = AnimationState.STARTING
        }
        invalidateSelf()
    }

    fun start() {
        startTime = SystemClock.elapsedRealtime()
        resume()
    }

    fun stop() {
        pause()
        pauseDuration = 0L
    }

    fun pause() {
        setPause(true)
    }

    fun resume() {
        setPause(false)
    }

    private fun setPause(pause: Boolean) {
        if (pause == this.pause) return
        this.pause = pause

        val now = SystemClock.elapsedRealtime()
        if (pause) {
            pauseTime = now
        } else {
            pauseDuration += (now - pauseTime)
        }
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val newHeight = bounds.height()
        if (currentHeight == 0) {
            currentHeight = newHeight
        }
        if (useAnim && nextResizeAnim) {
            sourceHeight = currentHeight
            targetHeight = newHeight
            resizeState = AnimationState.STARTING
            nextResizeAnim = false
        } else {
            sourceHeight = newHeight
            currentHeight = newHeight
            resizeState = AnimationState.DONE
        }
        invalidateSelf()
    }

    override fun draw(p0: Canvas) {
        if (runtimeShader == null || bounds.isEmpty) return

        val now = SystemClock.elapsedRealtime()

        when (colorState) {
            AnimationState.STARTING -> {
                colorStartTimeMillis = now
                colorState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (colorStartTimeMillis >= 0) {
                    val normalized: Float = ((now - colorStartTimeMillis) / COLOR_ANIM_DURATION).coerceIn(0.0f, 1.0f)
                    currentColor = argbEvaluator(normalized, sourceColor, targetColor)
                    if (normalized >= 1.0f) {
                        colorState = AnimationState.DONE
                        currentColor = targetColor
                    }
                }
            }
            else -> {}
        }
        when (resizeState) {
            AnimationState.STARTING -> {
                resizeStartTimeMillis = now
                resizeState = AnimationState.RUNNING
            }
            AnimationState.RUNNING -> {
                if (resizeStartTimeMillis >= 0) {
                    val normalized: Float = ((now - resizeStartTimeMillis) / RESIZE_ANIM_DURATION).coerceIn(0.0f, 1.0f)
                    currentHeight = linearInterpolate(sourceHeight, targetHeight, normalized)
                    if (normalized >= 1.0f) {
                        resizeState = AnimationState.DONE
                        currentHeight = targetHeight
                    }
                }
            }
            else -> {}
        }

        val w = bounds.width().toFloat()
        val h = currentHeight.toFloat()
        if (w > 0 && h > 0) {
            val duration = if (pause) {
                pauseTime - startTime - pauseDuration
            } else {
                now - startTime - pauseDuration
            }
            val time = duration / 1000f
            runtimeShader?.apply {
                setFloatUniform("uTime", time)
                setFloatUniform("uResolution", w, h)
                setFloatUniform("uGradientColorPositions", gradientPositions)
                setFloatUniform("uSdfRadius", 0f)

                setIntUniform("uIsLightMode", isLightMode)

                colorVec4[0] = Color.red(currentColor) / 255f
                colorVec4[1] = Color.green(currentColor) / 255f
                colorVec4[2] = Color.blue(currentColor) / 255f
                colorVec4[3] = (duration / LIGHT_ANIM_DURATION).coerceIn(0.0f, 1.0f)
                setFloatUniform("uGradientColor", colorVec4)
            }
            p0.drawRect(0.0f, 0.0f, w, h, paint)
        }

        if (isVisible && (!pause || colorState != AnimationState.DONE || resizeState != AnimationState.DONE)) {
            invalidateSelf()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setAlpha(p0: Int) {
        paint.alpha = p0
        invalidateSelf()
    }

    override fun setColorFilter(p0: ColorFilter?) {
        paint.colorFilter = p0
        invalidateSelf()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (visible) invalidateSelf()
        return changed
    }

    companion object {
        private const val LIGHT_ANIM_DURATION = 600.0f
        private const val COLOR_ANIM_DURATION = 600.0f
        private const val RESIZE_ANIM_DURATION = 234.0f

        private fun argbEvaluator(fraction: Float, startValue: Int, endValue: Int): Int {
            val startA = (startValue shr 24) and 0xff
            val startR = (startValue shr 16) and 0xff
            val startG = (startValue shr 8) and 0xff
            val startB = startValue and 0xff
            val endA = (endValue shr 24) and 0xff
            val endR = (endValue shr 16) and 0xff
            val endG = (endValue shr 8) and 0xff
            val endB = endValue and 0xff
            return ((startA + (fraction * (endA - startA)).toInt()) shl 24) or
                ((startR + (fraction * (endR - startR)).toInt()) shl 16) or
                ((startG + (fraction * (endG - startG)).toInt()) shl 8) or
                (startB + (fraction * (endB - startB)).toInt())
        }

        private const val AGSL_SRC = """
            uniform vec2 uResolution;
            uniform float uTime;
            uniform shader uTex;
            uniform float uSdfRadius;
            uniform vec4 uGradientColor;

            uniform vec2 uGradientColorPositions[3];
            uniform vec3 uGradientColors[3];

            uniform int uIsLightMode;

            float random(vec2 st) {
                return fract(sin(dot(st.xy,vec2(12.9898,78.233)))*43758.5453123);
            }

            // 2D noise based on Perlin's implementation
            float noise(vec2 st) {
                vec2 i = floor(st);
                vec2 f = fract(st);

                // Four corners in 2D of a tile
                float a = random(i);
                float b = random(i + vec2(1.0, 0.0));
                float c = random(i + vec2(0.0, 1.0));
                float d = random(i + vec2(1.0, 1.0));

                // Smooth interpolation using cubic Hermite curve
                vec2 u = f * f * (3.0 - 2.0 * f);

                // Mix 4 corners
                return mix(a, b, u.x) + (c - a)* u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
            }

            vec4 addBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + dst.rgb;
                float alpha = src.a + (1.0 - src.a) + dst.a;
                return vec4(color, alpha);
            }

            vec4 alphaBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + (1.0 - src.a) * dst.rgb;
                float alpha = src.a + dst.a * (1.0 - src.a);
                return vec4(color, alpha);
            }

            vec3 rgb2hsv(vec3 c) {
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

                float d = q.x - min(q.w, q.y);
                float e = 1e-10;
                return vec3(
                abs(q.z + (q.w - q.y) / (6.0 * d + e)), // h
                d / (q.x + e),                          // s：纯度
                q.x                                     // v
                );
            }

            vec3 hsv2rgb(vec3 c) {
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
            }

            vec4 main(vec2 fragCoord){
                vec2 vUv = fragCoord / uResolution;
                vUv.y = 1. - vUv.y;

                vec2 uv = vUv - 0.5;
                uv *= 1.0;
                uv.y *= uResolution.y / uResolution.x;
                uv += 0.5;
            //    uv.y = 1. - uv.y;

                vec4 color = vec4(0.0);

                vec2 pos = uv;

                float fx = 0.3;
                float fy = 2.0;
                float fa = 1.0;

                float ns[9];// = float[9](
                ns[0] = noise(vec2(1.0, uTime * fx));
                ns[1] = noise(vec2(2.0, uTime * fy));
                ns[2] = noise(vec2(1.5, uTime * fa));
                ns[3] = noise(vec2(3.0, uTime * fx));
                ns[4] = noise(vec2(4.0, uTime * fy));
                ns[5] = noise(vec2(3.5, uTime * fa));
                ns[6] = noise(vec2(5.0, uTime * fx));
                ns[7] = noise(vec2(6.0, uTime * fy));
                ns[8] = noise(vec2(5.5, uTime * fa));
                //);

                for (int i = 0; i< 3; i++) {
                    float ox = mix(-0.50, 0.50, ns[i*3+0]);
                    float oy = mix( 0.00, 0.03, ns[i*3+1]);

                    float oa = mix( 0.80, 1.00, ns[i*3+2]);
                    vec2 o = vec2(ox, oy);
                    vec2 delta = uGradientColorPositions[i] + o - pos;
                    // vec2 delta = uGradientColorPositions[i] - pos;
                    float dist = length(delta * 1.2);
                    float factor = dist;// clamp(1.0 - dist, 0.0, 1.0);

                    factor = smoothstep(2.0, 0.0, dist);
                    factor = pow(factor, 2.5);
                    factor = smoothstep(0.1, 1.0, factor);

                    factor += smoothstep(0.8, 0., vUv.y) * 0.2;


                    float s = factor * oa;

                    vec3 ballColor = uGradientColor.rgb;
                    float ballAlpha = uGradientColor.a;

                    ballColor = rgb2hsv(ballColor);

                    ballColor.x += (float(i) - 1.) * 0.08;
                    ballColor.y = 0.8;

                    ballColor.z += pow(factor, 2.) * 0.2;
                    if (uIsLightMode == 0) {
                        ballColor.z += pow(factor, 2.) * 0.2;
                    } else {
                        ballColor.z = 1.;
                    }
//                    if (uIsLightMode == 1) {
//                        ballColor.z *= max(ballColor.z, 0.9);
//                    } else {
//                        ballColor.z += pow(factor, 2.) * 0.2;
//                    }

                    ballColor = hsv2rgb(ballColor);
                    float mixBase = (uIsLightMode == 0) ? 0. : 1.;
                    float mixStrength = (uIsLightMode == 0) ? 0.8 : 0.9;
                    ballColor = mix(vec3(mixBase), ballColor, mixStrength);

                    if (uIsLightMode == 0) {
                        ballColor += pow(factor, 2.) * 0.2;
                        ballColor += smoothstep(0.3, 0., vUv.y) * 0.1 * oa;

                        ballColor = pow(ballColor, vec3(1.5));
                    } else {
                        ballColor = pow(ballColor, vec3(1.1));
                    }

                    float lightModeAlphaScale = (uIsLightMode == 1) ? 0.5 : 1.0;
                    float finalS = s * lightModeAlphaScale;
//
                    vec4 srcColor = vec4(ballColor * finalS, finalS);
//                    vec4 srcColor = vec4(ballColor * s, s);
                    // color = color * (1.0 - s) + lightColor;
                    // color = color * (1.0 - s) + srcColor;
                    color += srcColor * ballAlpha;
                }

                // color = min(vec3(1.0), color);
                color = min(vec4(1.0), color);
            //    color.rgb *= color.a;

            //    vec2 st = vUv;
            //    st -= 0.5;
            //    st.x *= uResolution.x / uResolution.y;
            //
            //    vec4 r = vec4(uSdfRadius/uResolution.y);
            //    vec2 s = vec2(uResolution.x/uResolution.y*0.5, 0.5);
            //    float sdf = rbx(st, s, r);
            //
            //    float alpha = smoothstep(1./uResolution.y, -1./uResolution.y, sdf);
            //    color *= alpha;

                return color;
            }
        """
    }
}
