package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool
import com.sevtinge.hyperceiler.module.hook.systemui.api.FlashlightController
import com.sevtinge.hyperceiler.module.hook.systemui.api.MiuiStub
import com.sevtinge.hyperceiler.module.hook.systemui.base.Keyguard.keyguardBottomAreaInjector
import com.sevtinge.hyperceiler.module.hook.systemui.base.Keyguard.leftButtonType
import com.sevtinge.hyperceiler.utils.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.setAdditionalInstanceField

object CustomizeBottomButton : BaseHook() {
    override fun init() {
        when (leftButtonType) {
            // 隐藏左侧按钮
            1 -> hideLeftButton()
            // 左侧按钮 -> 手电筒按钮
            2 -> replaceFlashlightButton()
        }
    }

    private fun hideLeftButton() {
        findAndHookMethod(keyguardBottomAreaInjector, "updateIcons", object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val leftButton = param.thisObject.getObjectFieldAs<ImageView?>("mLeftButton")
                leftButton?.setImageDrawable(null)
            }
        })
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

        hookAllConstructors(keyguardBottomAreaInjector, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                flashlightController = MiuiStub.sysUIProvider.flashlightController

                val context = MiuiStub.baseProvider.context
                ResourcesTool.loadModuleRes(context)
                offDrawable = context.getDrawable(R.drawable.ic_flashlight_off_filled)
                onDrawable = context.getDrawable(R.drawable.ic_flashlight_on_filled)
            }
        })

        findAndHookMethod(keyguardBottomAreaInjector, "updateLeftIcon", object : MethodHook() {
            override fun after(param: MethodHookParam) {
                leftButton = param.thisObject.getObjectFieldAs<ImageView?>("mLeftButton")?.also {
                    resetImageDrawable(false, false)
                    it.setOnTouchListener(touchListener)
                }
            }
        })
    }
}
