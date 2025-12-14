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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool.MethodHook.returnConstant
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.api.FlashlightController
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.lockscreen.Keyguard.keyguardBottomAreaInjector
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.lockscreen.Keyguard.leftButtonType
import com.sevtinge.hyperceiler.hook.utils.MethodHookParam
import com.sevtinge.hyperceiler.hook.utils.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.hook.utils.setAdditionalInstanceField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object CustomizeBottomButton : BaseHook() {
    override fun init() {
        when (leftButtonType) {
            // 隐藏左侧按钮
            1 -> hideLeftButton()
            // 左侧按钮 -> 手电筒按钮
            2 -> replaceFlashlightButton()
        }
    }

    fun hideLeftButton() {
        keyguardBottomAreaInjector.methodFinder()
            .filterByName("updateIcons")
            .single().createAfterHook {
                val left =
                    it.thisObject.getObjectFieldOrNullAs<LinearLayout>("mLeftAffordanceViewLayout") ?: return@createAfterHook
                left.visibility = View.GONE
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun replaceFlashlightButton() {
        lateinit var flashlightController: FlashlightController

        var downTime = 0L
        var leftButton: ImageView? = null
        var offDrawable: Drawable? = null
        var onDrawable: Drawable? = null

        val touchListener = View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = SystemClock.uptimeMillis()
                    view.animate().setDuration(100).scaleX(1.21F).scaleY(1.21F)
                }

                MotionEvent.ACTION_UP -> {
                    view.animate().setDuration(100).scaleX(1F).scaleY(1F).withEndAction {
                        if (SystemClock.uptimeMillis() - downTime >= 400) {
                            flashlightController.toggleFlashlight()
                            leftButton?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    view.animate().setDuration(150).scaleX(1F).scaleY(1F)
                }
            }

            true
        }

        val resetImageDrawable = { isEnabled: Boolean, isPost: Boolean ->
            leftButton?.run {
                val action = action@{
                    val isInnerEnabled = getAdditionalInstanceFieldAs<Boolean?>("isEnabled")
                    if (isInnerEnabled != null) {
                        if (isInnerEnabled == isEnabled) {
                            return@action
                        } else {
                            setAdditionalInstanceField("isEnabled", isEnabled)
                        }
                    }

                    setImageDrawable(if (isEnabled) onDrawable else offDrawable)
                }

                if (isPost) {
                    post(action)
                } else {
                    action()
                }
            }
        }

        val flashlightListener = object : FlashlightController.FlashlightListener {
            override fun onFlashlightError() {
                logD(TAG, lpparam.packageName, "onFlashlightError")
                resetImageDrawable(false, true)
            }

            override fun onFlashlightChanged(isEnabled: Boolean) {
                logD(TAG, lpparam.packageName, "onFlashlightChanged($isEnabled)")
                resetImageDrawable(isEnabled, true)
            }

            override fun onFlashlightAvailabilityChanged(isEnabled: Boolean) {
                logD(TAG, lpparam.packageName, "onFlashlightAvailabilityChanged($isEnabled)")
                if (!isEnabled) {
                    resetImageDrawable(false, true)
                }
            }
        }

        findAndHookMethod(
            "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl",
            "notifyKeyguardState", Boolean::class.java, Boolean::class.java,
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val showing = param.args[0] as Boolean
                    val occluded = param.args[1] as Boolean
                    logD(TAG, lpparam.packageName, "notifyKeyguardState($showing, $occluded)")

                    if (showing && !occluded) {
                        resetImageDrawable(flashlightController.isEnabled(), true)
                        FlashlightController.addListener(flashlightListener)
                    } else {
                        FlashlightController.removeListener(flashlightListener)
                    }
                }
            }
        )

        fun chargeImage(param: MethodHookParam, context: Context) {
            val mBottomIconRectIsDeep = param.thisObject.getObjectFieldAs<Boolean>("mBottomIconRectIsDeep")
            if (!mBottomIconRectIsDeep) {
                offDrawable = context.getDrawable(R.drawable.ic_flashlight_off_filled_black)
                onDrawable = context.getDrawable(R.drawable.ic_flashlight_on_filled_black)
            } else {
                offDrawable = context.getDrawable(R.drawable.ic_flashlight_off_filled)
                onDrawable = context.getDrawable(R.drawable.ic_flashlight_on_filled)
            }
        }

        hookAllConstructors(keyguardBottomAreaInjector, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                flashlightController = MiuiStub.sysUIProvider.flashlightController

                val context = MiuiStub.baseProvider.context
                OtherTool.getModuleRes(context)
                chargeImage(param, context)
            }
        })

        findAndHookMethod(
            keyguardBottomAreaInjector,
            "updateLeftIcon",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    leftButton =
                        param.thisObject.getObjectFieldAs<ImageView?>("mLeftButton")?.also {
                            resetImageDrawable(false, false)
                            it.setOnTouchListener(touchListener)
                        }
                    val context = MiuiStub.baseProvider.context
                    chargeImage(param, context)
                }
            })

        findAndHookMethod(
            "com.android.keyguard.negative.KeyguardMoveLeftController", "isLeftViewLaunchActivity",
            returnConstant(false)
        )
    }
}
