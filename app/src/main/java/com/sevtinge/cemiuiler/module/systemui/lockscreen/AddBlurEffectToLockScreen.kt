package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.animation.ValueAnimator
import android.graphics.drawable.LayerDrawable
import android.view.ViewGroup
import android.widget.ImageView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object AddBlurEffectToLockScreen : BaseHook() {
    override fun init() {
        val miuiNotificationPanelViewControllerClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController"
        ) ?: return

        val keyguardBottomAreaViewClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView"
        ) ?: return

        val keyguardMoveHelperClass = findClassIfExists(
            "com.android.keyguard.KeyguardMoveHelper"
        ) ?: return

        val baseKeyguardMoveHelperClass = findClassIfExists(
            "com.android.keyguard.BaseKeyguardMoveHelper"
        ) ?: return

        val lockScreenMagazineControllerClass = findClassIfExists(
            "com.android.keyguard.magazine.LockScreenMagazineController"
        ) ?: return

        //to com.sevtinge.cemiuiler.module.systemui.lockscreen.BlurButton
        /*XposedBridge.hookAllMethods(
            keyguardBottomAreaViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    /*if (!isDefaultLockScreenTheme()) {
                        return
                    }*/
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mRightAffordanceView"
                    ) as ImageView

                    val keyguardBottomAreaView = param.thisObject as View
                    val leftBlurDrawable = HookUtils.createBlurDrawable(
                        keyguardBottomAreaView,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
                    val rightBlurDrawable = HookUtils.createBlurDrawable(
                        keyguardBottomAreaView,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val rightLayerDrawable = LayerDrawable(arrayOf(rightBlurDrawable))
                    leftLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    rightLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    mLeftAffordanceView.background = leftLayerDrawable
                    mRightAffordanceView.background = rightLayerDrawable
                }
            })*/

        XposedBridge.hookAllMethods(
            miuiNotificationPanelViewControllerClass,
            "setBouncerShowingFraction",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val mKeyguardBouncerShowing = HookUtils.getValueByField(
                        param.thisObject,
                        "mKeyguardBouncerShowing"
                    ) as Boolean
                    val mKeyguardBottomArea =
                        XposedHelpers.callMethod(param.thisObject, "getKeyguardBottomArea")
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mRightAffordanceView"
                    ) as ImageView
                    if (mLeftAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mLeftAffordanceView.background as LayerDrawable
                        val blurDrawable = layerDrawable.getDrawable(0)
                        XposedHelpers.callMethod(
                            blurDrawable,
                            "setVisible",
                            !mKeyguardBouncerShowing,
                            false
                        )
                    }
                    if (mRightAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mRightAffordanceView.background as LayerDrawable
                        val blurDrawable = layerDrawable.getDrawable(0)
                        XposedHelpers.callMethod(
                            blurDrawable,
                            "setVisible",
                            !mKeyguardBouncerShowing,
                            false
                        )
                    }
                }
            })

        XposedBridge.hookAllMethods(
            miuiNotificationPanelViewControllerClass,
            "updateKeyguardElementAlpha",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    val mKeyguardBouncerShowing = HookUtils.getValueByField(
                        param.thisObject,
                        "mKeyguardBouncerShowing"
                    ) ?: return
                    mNotificationStackScroller as ViewGroup
                    mKeyguardBouncerShowing as Boolean
                    val keyguardContentsAlpha =
                        XposedHelpers.callMethod(
                            param.thisObject,
                            "getKeyguardContentsAlpha"
                        ) as Float
                    val drawableAlpha = keyguardContentsAlpha * 255
                    val mKeyguardBottomArea =
                        XposedHelpers.callMethod(param.thisObject, "getKeyguardBottomArea")
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mRightAffordanceView"
                    ) as ImageView
                    if (mLeftAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mLeftAffordanceView.background as LayerDrawable
                        val blurDrawable = layerDrawable.getDrawable(0)
                        XposedHelpers.callMethod(
                            blurDrawable,
                            "setAlpha",
                            drawableAlpha.toInt()
                        )
                    }
                    if (mRightAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mRightAffordanceView.background as LayerDrawable
                        val blurDrawable = layerDrawable.getDrawable(0)
                        XposedHelpers.callMethod(
                            blurDrawable,
                            "setAlpha",
                            drawableAlpha.toInt()
                        )
                    }
                }
            })

        //控制中心异常
        /*XposedBridge.hookAllMethods(
            keyguardMoveHelperClass,
            "setTranslation",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val mCurrentScreen =
                        HookUtils.getValueByField(param.thisObject, "mCurrentScreen") as Int
                    val mLeftViewBg =
                        HookUtils.getValueByField(param.thisObject, "mLeftViewBg") as ImageView
                    mLeftViewBg.setImageDrawable(null)
                    mLeftViewBg.setBackgroundColor(Color.TRANSPARENT)
                    val transitionX = param.args[0] as Float
                    val screenWidth =
                        XposedHelpers.callMethod(
                            param.thisObject,
                            "getScreenWidth"
                        ) as Float
                    var alpha = (transitionX / screenWidth) * 255
                    if (mCurrentScreen != 1) {
                        alpha += 255
                    }
                    if (alpha > 255 || alpha < 0) {
                        return
                    }
                    val drawableAlpha = (255 - alpha).toInt()
                    var blurRadius = (transitionX / screenWidth) * 80
                    var colorAlpha = (transitionX / screenWidth) * 50
                    if (mCurrentScreen != 1) {
                        blurRadius += 80
                        colorAlpha += 50
                    }
                    val mFaceUnlockView =
                        HookUtils.getValueByField(param.thisObject, "mFaceUnlockView") ?: return
                    mFaceUnlockView as View
                    if (mFaceUnlockView.parent == null || mFaceUnlockView.parent.parent == null) {
                        return
                    }
                    val targetView = mFaceUnlockView.parent.parent as View
                    if (blurRadius in 1f..81f) {
                        if (HookUtils.isBlurDrawable(targetView.background)) {
                            XposedHelpers.callMethod(
                                targetView.background,
                                "setBlurRadius",
                                blurRadius.toInt()
                            )
                            XposedHelpers.callMethod(
                                targetView.background,
                                "setColor",
                                Color.argb(colorAlpha.toInt(), 0, 0, 0)
                            )
                        } else {
                            targetView.background =
                                HookUtils.createBlurDrawable(
                                    targetView,
                                    blurRadius.toInt(),
                                    0,
                                    Color.argb(colorAlpha.toInt(), 0, 0, 0)
                                )
                        }
                    } else {
                        targetView.background = null
                    }
                    val mNotificationPanelViewController =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationPanelViewController"
                        )
                            ?: return
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            mNotificationPanelViewController,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup

                    val mKeyguardBottomArea =
                        XposedHelpers.callMethod(
                            mNotificationPanelViewController,
                            "getKeyguardBottomArea"
                        )
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mRightAffordanceView"
                    ) as ImageView
                    if (mLeftAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mLeftAffordanceView.background as LayerDrawable
                        layerDrawable.alpha = drawableAlpha
                    }
                    if (mRightAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mRightAffordanceView.background as LayerDrawable
                        layerDrawable.alpha = drawableAlpha
                    }
                }
            })
         */

        //锁屏过渡至桌面模糊异常
        /*
        XposedBridge.hookAllMethods(
            baseKeyguardMoveHelperClass,
            "doPanelViewAnimation",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val mNotificationPanelViewController =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationPanelViewController"
                        )
                            ?: return
                    val panelView = XposedHelpers.callMethod(
                        mNotificationPanelViewController,
                        "getPanelView"
                    ) ?: return
                    panelView as View
                    if (panelView.parent == null) {
                        return
                    }
                    val targetBlurView = panelView.parent as View
                    val isOnKeyguard = XposedHelpers.callMethod(
                        mNotificationPanelViewController,
                        "isOnKeyguard"
                    ) as Boolean
                    if (!isOnKeyguard) {
                        if (HookUtils.isBlurDrawable(targetBlurView.background)) {
                            targetBlurView.background = null
                        }
                        return
                    }
                    val mKeyguardBouncerShowing = HookUtils.getValueByField(
                        mNotificationPanelViewController,
                        "mKeyguardBouncerShowing"
                    ) as Boolean
                    if (mKeyguardBouncerShowing) {
                        return
                    }
                    val mTranslationPer =
                        HookUtils.getValueByField(param.thisObject, "mTranslationPer") as Float
                    val f2 = 1.0f - (1.0f - mTranslationPer) * (1.0f - mTranslationPer)
                    val blurRadius = f2 * 50
                    if (blurRadius > 0f) {
                        if (HookUtils.isBlurDrawable(targetBlurView.background)) {
                            XposedHelpers.callMethod(
                                targetBlurView.background,
                                "setBlurRadius",
                                blurRadius.toInt()
                            )
                        } else {
                            targetBlurView.background =
                                HookUtils.createBlurDrawable(targetBlurView, blurRadius.toInt(), 0)
                        }
                    } else if (!mKeyguardBouncerShowing) {
                        targetBlurView.background = null
                    }
                }
            })

         */

        XposedBridge.hookAllMethods(
            miuiNotificationPanelViewControllerClass,
            "onBouncerShowingChanged",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val isBouncerShowing = param.args[0] as Boolean
                    val mBouncerFractionAnimator =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mBouncerFractionAnimator"
                        ) ?: return
                    mBouncerFractionAnimator as ValueAnimator
                    if (isBouncerShowing) {
                        val mKeyguardBouncerFractionField =
                            miuiNotificationPanelViewControllerClass.getDeclaredField("mKeyguardBouncerFraction")
                        mKeyguardBouncerFractionField.isAccessible = true
                        mKeyguardBouncerFractionField.set(param.thisObject, 1f)
                    }
                }
            })

        XposedBridge.hookAllMethods(
            lockScreenMagazineControllerClass,
            "setViewsAlpha",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val alpha = param.args[0] as Float
                    val drawableAlpha = alpha * 255
                    if (drawableAlpha < 0 || drawableAlpha > 255) {
                        return
                    }
                    val mNotificationStackScrollLayout = HookUtils.getValueByField(
                        param.thisObject,
                        "mNotificationStackScrollLayout"
                    ) ?: return
                    // NotificationStackScrollLayoutController
                    val mController =
                        HookUtils.getValueByField(mNotificationStackScrollLayout, "mController")
                            ?: return

                    val mPanelViewController =
                        HookUtils.getValueByField(mController, "mPanelViewController") ?: return

                    val mKeyguardBottomArea =
                        XposedHelpers.callMethod(
                            mPanelViewController,
                            "getKeyguardBottomArea"
                        ) ?: return
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        mKeyguardBottomArea,
                        "mRightAffordanceView"
                    ) as ImageView
                    if (mLeftAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mLeftAffordanceView.background as LayerDrawable
                        layerDrawable.alpha = drawableAlpha.toInt()
                    }
                    if (mRightAffordanceView.background is LayerDrawable) {
                        val layerDrawable = mRightAffordanceView.background as LayerDrawable
                        layerDrawable.alpha = drawableAlpha.toInt()
                    }
                }
            })

        XposedBridge.hookAllMethods(
            keyguardBottomAreaViewClass,
            "setDozing",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val isDozing = param.args[0] as Boolean
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mRightAffordanceView"
                    ) as ImageView
                    if (mLeftAffordanceView.background == null || mRightAffordanceView.background == null) {
                        return
                    }
                    val leftLayerDrawable = mLeftAffordanceView.background
                    val rightLayerDrawable = mRightAffordanceView.background
                    if (leftLayerDrawable is LayerDrawable && rightLayerDrawable is LayerDrawable) {
                        val leftBlurDrawable = leftLayerDrawable.getDrawable(0) ?: return
                        val rightBlurDrawable = rightLayerDrawable.getDrawable(0) ?: return
                        XposedHelpers.callMethod(
                            leftBlurDrawable,
                            "setVisible",
                            !isDozing,
                            false
                        )
                        XposedHelpers.callMethod(
                            rightBlurDrawable,
                            "setVisible",
                            !isDozing,
                            false
                        )
                    }
                }
            })

    }

    fun isDefaultLockScreenTheme(): Boolean {
        val miuiKeyguardUtilsClass = findClassIfExists(
            "com.android.keyguard.utils.MiuiKeyguardUtils"
        ) ?: return true
        return XposedHelpers.callStaticMethod(
            miuiKeyguardUtilsClass,
            "isDefaultLockScreenTheme"
        ) as Boolean
    }

}