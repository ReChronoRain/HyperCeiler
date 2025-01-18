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
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.blur.BlurUtils.*
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.utils.devicesdk.*
import de.robv.android.xposed.*

object BlurButton : BaseHook() {
    private val removeLeft by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_hide_smart_screen")
    }
    private val removeRight by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_hide_camera")
    }
    private val radius by lazy {
        mPrefsMap.getInt("system_ui_lock_screen_blur_button_radius", 40)
    }
    private val hyperBlur by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_hyper_blur_button")
    }
    private val blurBotton by lazy {
        isTransparencyLow(mPrefsMap.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
    }

    override fun init() {
        // by StarVoyager
        loadClassOrNull("com.android.keyguard.injector.KeyguardBottomAreaInjector")!!.methodFinder()
            .filter {
                name in setOf(
                    "updateLeftIcon",
                    "updateRightIcon"
                )
            }.toList().createHooks {
                after { param ->
                    try {
                        if (hyperBlur) hyperBlur(param) else systemBlur(param)
                        if (blurBotton) param.thisObject.setBooleanField(
                            "mBottomIconRectIsDeep",
                            isColorDark(mPrefsMap.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
                        )
                    } catch (_: Throwable) {
                    }
                }
            }
    }

    private fun setNewBackgroundBlur(imageView: ImageView): LayerDrawable {
        val blurDrawable = createBlurDrawable(
            imageView, 40, 100, Color.argb(60, 255, 255, 255)
        )
        val layoutDrawable = LayerDrawable(arrayOf(blurDrawable))
        layoutDrawable.setLayerInset(0, radius, radius, radius, radius)
        return layoutDrawable
    }

    private fun setOldBackgroundBlur(view: View): LayerDrawable {
        val blurDrawable = createBlurDrawable(
            view, 40, 100, Color.argb(60, 255, 255, 255)
        )
        val layoutDrawable = LayerDrawable(arrayOf(blurDrawable))
        layoutDrawable.setLayerInset(0, radius, radius, radius, radius)
        return layoutDrawable
    }

    private fun systemBlur(param: XC_MethodHook.MethodHookParam) {
        val mLeftAffordanceView: ImageView = ObjectUtils.getObjectOrNullAs<ImageView>(
            param.thisObject,
            "mLeftButton"
        )!!
        val mRightAffordanceView: ImageView = ObjectUtils.getObjectOrNullAs<ImageView>(
            param.thisObject,
            "mRightButton"
        )!!
        // Your blur logic
        val context = ObjectUtils.getObjectOrNull(param.thisObject, "mContext") as Context
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isKeyguardLocked) {
            mLeftAffordanceView.background = if (!removeLeft) {
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

    private fun hyperBlur(param: XC_MethodHook.MethodHookParam) {
        val mLeftAffordanceView: ImageView = ObjectUtils.getObjectOrNullAs<ImageView>(
            param.thisObject,
            "mLeftButton"
        )!!
        val mRightAffordanceView: ImageView = ObjectUtils.getObjectOrNullAs<ImageView>(
            param.thisObject,
            "mRightButton"
        )!!

        if (!removeLeft) {
            addHyBlur(mLeftAffordanceView)
        }
        if (!removeRight) {
            addHyBlur(mRightAffordanceView)
        }
    }

    private fun addHyBlur(view: ImageView) {
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
            setMiViewBlurMode(1)
            setMiBackgroundBlurRadius(40)
            addMiBackgroundBlendColor(mPrefsMap.getInt("system_ui_lock_screen_blur_button_bg_color", 0), 101)
        }
    }

    private fun mapValueToRange(dynamicValue: Int): Int {
        return 60 + ((dynamicValue - 10) * 60 / 50)
    }

    private fun isTransparencyLow(color: Int): Boolean {
        val alpha = (color shr 24) and 0xFF
        return alpha > 92
    }

    private fun isColorDark(color: Int): Boolean {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF

        val brightness = 0.299 * red + 0.587 * green + 0.114 * blue

        return brightness < 128
    }

}
