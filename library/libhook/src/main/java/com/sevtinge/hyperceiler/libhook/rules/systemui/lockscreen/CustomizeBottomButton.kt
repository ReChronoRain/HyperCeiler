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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.FlashlightController
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard.keyguardBottomAreaInjector
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard.leftButtonType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MiuiStub
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam
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
                XposedLog.d(TAG, lpparam.packageName, "onFlashlightError")
                resetImageDrawable(false, true)
            }

            override fun onFlashlightChanged(isEnabled: Boolean) {
                XposedLog.d(TAG, lpparam.packageName, "onFlashlightChanged($isEnabled)")
                resetImageDrawable(isEnabled, true)
            }

            override fun onFlashlightAvailabilityChanged(isEnabled: Boolean) {
                XposedLog.d(TAG, lpparam.packageName, "onFlashlightAvailabilityChanged($isEnabled)")
                if (!isEnabled) {
                    resetImageDrawable(false, true)
                }
            }
        }

        loadClass("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl")
            .afterHookMethod(
                "notifyKeyguardState",
                Boolean::class.java, Boolean::class.java
            ) { param ->
                val showing = param.args[0] as Boolean
                val occluded = param.args[1] as Boolean
                XposedLog.d(TAG, lpparam.packageName, "notifyKeyguardState($showing, $occluded)")

                if (showing && !occluded) {
                    resetImageDrawable(flashlightController.isEnabled(), true)
                    FlashlightController.addListener(flashlightListener)
                } else {
                    FlashlightController.removeListener(flashlightListener)
                }
            }

        fun chargeImage(param: AfterHookParam, context: Context) {
            val mBottomIconRectIsDeep = param.thisObject.getObjectFieldAs<Boolean>("mBottomIconRectIsDeep")
            if (!mBottomIconRectIsDeep) {
                offDrawable = context.getDrawable(R.drawable.ic_flashlight_off_filled_black)
                onDrawable = context.getDrawable(R.drawable.ic_flashlight_on_filled_black)
            } else {
                offDrawable = context.getDrawable(R.drawable.ic_flashlight_off_filled)
                onDrawable = context.getDrawable(R.drawable.ic_flashlight_on_filled)
            }
        }

        keyguardBottomAreaInjector.apply {
            hookAllConstructors {
                after { param ->
                    flashlightController = MiuiStub.sysUIProvider.flashlightController

                    val context = MiuiStub.baseProvider.context
                    AppsTool.getModuleRes(context)
                    chargeImage(param, context)
                }
            }

            afterHookMethod("updateLeftIcon") { param ->
                leftButton =
                    param.thisObject.getObjectFieldAs<ImageView?>("mLeftButton")?.also {
                        resetImageDrawable(false, false)
                        it.setOnTouchListener(touchListener)
                    }
                val context = MiuiStub.baseProvider.context
                chargeImage(param, context)
            }
        }

        loadClass("com.android.keyguard.negative.KeyguardMoveLeftController")
            .replaceMethod("isLeftViewLaunchActivity") {
                false
            }

    }
}
