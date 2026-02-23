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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils.createBlurDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlurMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setPassWindowBlurEnabled
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard.keyguardBottomAreaInjector
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard.leftButtonType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setBooleanField
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks

object BlurButton : BaseHook() {
    private val removeRight by lazy {
        PrefsBridge.getBoolean("system_ui_lock_screen_hide_camera")
    }
    private val radius by lazy {
        PrefsBridge.getInt("system_ui_lock_screen_blur_button_radius", 40)
    }
    private val hyperBlur by lazy {
        PrefsBridge.getBoolean("system_ui_lock_screen_hyper_blur_button")
    }
    private val blurBotton by lazy {
        isTransparencyLow(PrefsBridge.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
    }

    override fun init() {
        // by StarVoyager
        keyguardBottomAreaInjector.methodFinder()
            .filter {
                name in setOf(
                    "updateLeftIcon",
                    "updateRightIcon"
                )
            }.toList().createHooks {
                after { param ->
                    runCatching {
                        if (hyperBlur) hyperBlur(param) else systemBlur(param)
                        if (blurBotton) param.thisObject.setBooleanField(
                            "mBottomIconRectIsDeep",
                            isColorDark(PrefsBridge.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
                        )
                    }
                }
            }
    }

    private fun systemBlur(param: AfterHookParam) {
        val mLeftAffordanceView: ImageView =
            param.thisObject.getObjectFieldOrNullAs<ImageView>("mLeftButton")!!
        val mRightAffordanceView: ImageView =
            param.thisObject.getObjectFieldOrNullAs<ImageView>("mRightButton")!!
        // Your blur logic
        val context = param.thisObject.getObjectFieldAs("mContext") as Context
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isKeyguardLocked) {
            mLeftAffordanceView.background = if (leftButtonType != 1) {
                setNewBackgroundBlur(mLeftAffordanceView)
            } else null
            mRightAffordanceView.background = if (!removeRight) {
                setNewBackgroundBlur(mRightAffordanceView)
            } else null
        } else {
            mLeftAffordanceView.background = null
            mRightAffordanceView.background = null
        }

    }

    private fun hyperBlur(param: AfterHookParam) {
        val mLeftAffordanceView: ImageView =
            param.thisObject.getObjectFieldOrNullAs<ImageView>("mLeftButton")!!
        val mRightAffordanceView: ImageView =
            param.thisObject.getObjectFieldOrNullAs<ImageView>("mRightButton")!!

        if (leftButtonType != 1) {
            addHyBlur(mLeftAffordanceView)
        }
        if (!removeRight) {
            addHyBlur(mRightAffordanceView)
        }
    }

    fun setNewBackgroundBlur(imageView: ImageView): LayerDrawable {
        val blurDrawable = createBlurDrawable(
            imageView, 40, 100, Color.argb(60, 255, 255, 255)
        )
        val layoutDrawable = LayerDrawable(arrayOf(blurDrawable))
        layoutDrawable.setLayerInset(0, radius, radius, radius, radius)
        return layoutDrawable
    }

    fun addHyBlur(view: ImageView) {
        val hyRadius = mapValueToRange(radius)

        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(
                    (view.width / 2) - hyRadius,
                    (view.height / 2) - hyRadius,
                    (view.width / 2) + hyRadius,
                    (view.height / 2) + hyRadius
                )
            }
        }

        // 启用裁剪
        view.clipToOutline = true
        view.apply {
            clearMiBackgroundBlendColor()
            setPassWindowBlurEnabled(true)
            setMiViewBlurMode(1)
            setMiBackgroundBlurMode(1)
            setMiBackgroundBlurRadius(100)
            addMiBackgroundBlendColor(PrefsBridge.getInt("system_ui_lock_screen_blur_button_bg_color", 0), 101)
        }
    }

    private fun mapValueToRange(dynamicValue: Int): Int {
        return 60 + ((dynamicValue - 10) * 60 / 50)
    }

    fun isTransparencyLow(color: Int): Boolean {
        val alpha = (color shr 24) and 0xFF
        return alpha > 92
    }

    fun isColorDark(color: Int): Boolean {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF

        val brightness = 0.299 * red + 0.587 * green + 0.114 * blue

        return brightness < 128
    }

}
