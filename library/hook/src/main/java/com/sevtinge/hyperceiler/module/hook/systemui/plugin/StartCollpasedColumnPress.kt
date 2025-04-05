package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.animation.*
import android.os.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils.*
import de.robv.android.xposed.*

object StartCollpasedColumnPress {
    fun initLoaderHook(classLoader: ClassLoader) {
        val miuiVolumeDialogView by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogView", classLoader)
        }
        val miuiVolumeDialogMotion by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogMotion", classLoader)
        }
        val miuiVolumeSeekBar by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeSeekBar", classLoader)
        }

        XposedHelpers.findAndHookMethod(
            miuiVolumeDialogView, "onFinishInflate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val thisObj = param?.thisObject
                    val mExpandButton =
                        XposedHelpers.getObjectField(thisObj, "mExpandButton") as View
                    mExpandButton.setOnClickListener(null)
                    mExpandButton.alpha = 0f
                    mExpandButton.isClickable = false
                    mExpandButton.visibility = View.GONE
                }
            })

        XposedHelpers.findAndHookMethod(
            miuiVolumeDialogView, "notifyAccessibilityChanged", Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val thisObj = param?.thisObject
                    val mExpandButton =
                        XposedHelpers.getObjectField(thisObj, "mExpandButton") as View

                    mExpandButton.setOnClickListener(null)
                    mExpandButton.isClickable = false
                    mExpandButton.visibility = View.GONE
                }
            })

        XposedBridge.hookAllConstructors(miuiVolumeDialogMotion, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val thisObj = param?.thisObject
                val mExpandButton = XposedHelpers.getObjectField(thisObj, "mExpandButton") as View

                mExpandButton.setOnTouchListener(null)
            }
        })


        var longClick = false
        XposedHelpers.findAndHookMethod(
            miuiVolumeDialogMotion, "lambda\$processExpandTouch\$1",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    val thisObj = param?.thisObject
                    if (XposedHelpers.getBooleanField(thisObj, "mExpanded") || !longClick) return null
                    val mVolumeView = XposedHelpers.getObjectField(thisObj, "mVolumeView") as View

                    logD("StartCollpasedColumnPress", "miui.systemui.plugin", "processExpandTouch")

                    with(AnimatorSet()) {
                        playTogether(
                            ObjectAnimator.ofFloat(mVolumeView, "scaleX", 0.95f),
                            ObjectAnimator.ofFloat(mVolumeView, "scaleY", 0.95f)
                        )
                        duration = 100L
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                mVolumeView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                val mVolumeExpandCollapsedAnimator =
                                    XposedHelpers.getObjectField(thisObj, "mVolumeExpandCollapsedAnimator")
                                val mCallback = XposedHelpers.getObjectField(thisObj, "mCallback")
                                XposedHelpers.callMethod(mVolumeExpandCollapsedAnimator, "calculateFromViewValues", true)
                                XposedHelpers.callMethod(mCallback, "onExpandClicked")

                                mVolumeView.scaleX = 1f
                                mVolumeView.scaleY = 1f
                            }
                        })
                        start()
                    }
                    return null
                }

            })

        XposedHelpers.findAndHookMethod(
            miuiVolumeSeekBar, "onTouchEvent", MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val thisObj = param?.thisObject
                    val mSeekBarOnclickListener =
                        XposedHelpers.getObjectField(thisObj, "mSeekBarOnclickListener")

                    val handler = Handler(Looper.getMainLooper())
                    val mLongPressRunnable = Runnable {
                        val mMoveY = XposedHelpers.getFloatField(thisObj, "mMoveY")
                        if (longClick) {
                            thisObj as View
                            XposedHelpers.callMethod(mSeekBarOnclickListener, "onClick")
                        }
                    }

                    if (mSeekBarOnclickListener != null) {
                        val motionEvent = param?.args?.get(0) as MotionEvent

                        val action = motionEvent.action
                        when (action) {
                            0 -> {
                                longClick = true
                                XposedHelpers.setLongField(thisObj, "mCurrentMS", 0L)
                                handler.postDelayed(mLongPressRunnable, 300L)
                            }

                            1 -> {
                                longClick = false
                                XposedHelpers.setLongField(thisObj, "mCurrentMS", 0L)
                            }

                            2 -> {
                                longClick = false
                            }
                        }
                    }
                }
            })
    }
}
