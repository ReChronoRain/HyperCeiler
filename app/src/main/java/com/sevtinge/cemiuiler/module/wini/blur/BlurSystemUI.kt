package com.sevtinge.cemiuiler.module.wini.blur

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.sevtinge.cemiuiler.module.wini.model.ConfigModel
import com.sevtinge.cemiuiler.utils.HookUtils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class BlurSystemUI (private val classLoader: ClassLoader, config: ConfigModel) {
    val cornerRadius = config.BlurSystemUI.notification.cornerRadius
    val blurRadius = config.BlurSystemUI.notification.blurRadius
    val blurBackgroundAlpha = config.BlurSystemUI.notification.blurBackgroundAlpha
    val defaultBackgroundAlpha = config.BlurSystemUI.notification.defaultBackgroundAlpha
    val qsControlDetailBackgroundAlpha = config.BlurSystemUI.quickSetting.controlDetailBackgroundAlpha

    fun addBlurEffectToNotificationView() {
        val MiuiExpandableNotificationRowClass = HookUtils.getClass(
            "com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow",
            classLoader
        ) ?: return

        val NotificationBackgroundViewClass = HookUtils.getClass(
            "com.android.systemui.statusbar.notification.row.NotificationBackgroundView",
            classLoader
        ) ?: return

        val AppMiniWindowRowTouchHelperClass = HookUtils.getClass(
            "com.android.systemui.statusbar.notification.policy.AppMiniWindowRowTouchHelper",
            classLoader
        ) ?: return

        val MiuiNotificationPanelViewControllerClass = HookUtils.getClass(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController",
            classLoader
        ) ?: return

        val NotificationStackScrollLayoutClass = HookUtils.getClass(
            "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout",
            classLoader
        ) ?: return

        val LockScreenMagazineControllerClass = HookUtils.getClass(
            "com.android.keyguard.magazine.LockScreenMagazineController",
            classLoader
        ) ?: return

        val BlurRatioChangedListener = HookUtils.getClass(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController\$mBlurRatioChangedListener\$1",
            classLoader
        ) ?: return


        // 每次设置背景的时候都同时改透明度
        XposedBridge.hookAllMethods(
            NotificationBackgroundViewClass,
            "setCustomBackground",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val notificationBackgroundView = param.thisObject
                    val mDrawableAlphaField =
                        NotificationBackgroundViewClass.getDeclaredField("mDrawableAlpha")
                    mDrawableAlphaField.isAccessible = true
                    val isHandsUp =
                        XposedHelpers.callMethod(notificationBackgroundView, "headsUp") as Boolean
                    if (isHandsUp) {
                        mDrawableAlphaField.set(notificationBackgroundView, blurBackgroundAlpha)
                        XposedHelpers.callMethod(
                            notificationBackgroundView,
                            "setDrawableAlpha",
                            blurBackgroundAlpha
                        )
                    } else {
                        mDrawableAlphaField.set(notificationBackgroundView, defaultBackgroundAlpha)
                        XposedHelpers.callMethod(
                            notificationBackgroundView,
                            "setDrawableAlpha",
                            defaultBackgroundAlpha
                        )
                    }
                }
            })

        // 背景bounds改动同步到模糊
        XposedBridge.hookAllMethods(
            NotificationBackgroundViewClass,
            "draw",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val notificationBackground = param.thisObject as View
                    val backgroundDrawable = notificationBackground.background ?: return
                    if (HookUtils.isBlurDrawable(backgroundDrawable)) {
                        val drawable = param.args[1] as Drawable
                        backgroundDrawable.bounds = drawable.bounds
                    }
                }
            })

        // 进入小窗模式的时候把模糊去掉
        XposedBridge.hookAllMethods(
            AppMiniWindowRowTouchHelperClass,
            "onMiniWindowTrackingStart",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val mPickedMiniWindowChild =
                        HookUtils.getValueByField(param.thisObject, "mPickedMiniWindowChild")
                            ?: return

                    val mBackgroundNormal =
                        HookUtils.getValueByField(mPickedMiniWindowChild, "mBackgroundNormal")
                            ?: return
                    mBackgroundNormal as View

                    if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                        XposedHelpers.callMethod(
                            mBackgroundNormal.background,
                            "setVisible",
                            false,
                            false
                        )
                        XposedHelpers.callMethod(
                            mBackgroundNormal,
                            "setDrawableAlpha",
                            defaultBackgroundAlpha + 30
                        )
                    }
                }
            })

        XposedBridge.hookAllMethods(
            AppMiniWindowRowTouchHelperClass,
            "onMiniWindowReset",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val mPickedMiniWindowChild =
                        HookUtils.getValueByField(param.thisObject, "mPickedMiniWindowChild")
                            ?: return

                    val mBackgroundNormal =
                        HookUtils.getValueByField(mPickedMiniWindowChild, "mBackgroundNormal")
                            ?: return
                    mBackgroundNormal as View

                    if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                        XposedHelpers.callMethod(
                            mBackgroundNormal.background,
                            "setVisible",
                            true,
                            false
                        )
                        XposedHelpers.callMethod(
                            mBackgroundNormal,
                            "setDrawableAlpha",
                            blurBackgroundAlpha
                        )
                    }
                }
            })

        // 悬浮的时候把模糊加上
        XposedBridge.hookAllMethods(
            MiuiExpandableNotificationRowClass,
            "setHeadsUp",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val isHeadsUp = param.args[0] as Boolean
                    val miuiNotificationBackgroundView = param.thisObject as View
                    val mBackgroundNormal = HookUtils.getValueByField(
                        miuiNotificationBackgroundView,
                        "mBackgroundNormal"
                    ) as View
                    if (!mBackgroundNormal.isAttachedToWindow) {
                        return;
                    }
                    if (isHeadsUp) {
                        if (mBackgroundNormal.background != null) {
                            if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                                return;
                            }
                        }
                        mBackgroundNormal.background =
                            HookUtils.createBlurDrawable(
                                mBackgroundNormal,
                                blurRadius,
                                cornerRadius
                            )

                        XposedHelpers.callMethod(
                            mBackgroundNormal,
                            "setDrawableAlpha",
                            blurBackgroundAlpha
                        )
                    } else {
                        if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                            mBackgroundNormal.background = null
                        }
                        try {
                            XposedHelpers.callMethod(
                                mBackgroundNormal,
                                "setDrawableAlpha",
                                defaultBackgroundAlpha
                            )
                        } catch (e: Throwable) {
                            //
                        }
                    }
                }
            })

        // 进入不同状态，处理一下模糊
        XposedBridge.hookAllMethods(
            MiuiNotificationPanelViewControllerClass,
            "onStateChanged",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // status 1 锁屏 2 锁屏下拉 0 其他
                    val status = param.args[0] as Int
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) as ViewGroup
                    if (status == 1) {
                        if (!isDefaultLockScreenTheme()) {
                            return
                        }
                        for (i in 0..mNotificationStackScroller.childCount) {
                            val childAt =
                                mNotificationStackScroller.getChildAt(i) ?: continue
                            showBlurEffectForNotificationRow(childAt)
                        }
                    } else {
                        for (i in 0..mNotificationStackScroller.childCount) {
                            val childAt =
                                mNotificationStackScroller.getChildAt(i) ?: continue
                            try {
                                val isHeadsUp =
                                    XposedHelpers.callMethod(childAt, "isHeadsUpState") as Boolean
                                val isPinned = XposedHelpers.callMethod(
                                    childAt,
                                    "isPinned"
                                ) as Boolean
                                if (isHeadsUp && isPinned) {
                                    showBlurEffectForNotificationRow(childAt)
                                } else {
                                    hideBlurEffectForNotificationRow(childAt)
                                }
                            } catch (e: Throwable) {
                                hideBlurEffectForNotificationRow(childAt)
                            }
                        }
                    }
                }
            })

        // 下拉完成处理模糊
        /*
        XposedBridge.hookAllMethods(
            NotificationPanelViewControllerClass,
            "onExpandingFinished",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val isOnKeyguard =
                        XposedHelpers.callMethod(param.thisObject, "isOnKeyguard") as Boolean
                    if (isOnKeyguard) {
                        return
                    }
                    val mNotificationStackScroller =
                        Hook.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    for (i in 0..mNotificationStackScroller.childCount) {
                        val childAt =
                            mNotificationStackScroller.getChildAt(i) ?: continue
                        hideBlurEffectForNotificationRow(childAt)
                    }
                }
            })
         */

        // 通知添加进视图的时候增加模糊
        XposedBridge.hookAllMethods(
            NotificationStackScrollLayoutClass,
            "onViewAddedInternal",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val expandableView = param.args[0] as View
                    val mController = HookUtils.getValueByField(param.thisObject, "mController")
                        ?: return
                    val mPanelViewController =
                        HookUtils.getValueByField(mController, "mPanelViewController")
                            ?: return
                    val isExpanding =
                        XposedHelpers.callMethod(mPanelViewController, "isExpanding") as Boolean
                    if (isExpanding) {
                        return
                    }
                    val isOnKeyguard =
                        XposedHelpers.callMethod(mPanelViewController, "isOnKeyguard") as Boolean
                    if (isOnKeyguard) {
                        if (!isDefaultLockScreenTheme()) {
                            return
                        }
                        showBlurEffectForNotificationRow(expandableView)
                    } else {
                        // ZenModeView 没有 isHeadsUpState 方法
                        try {
                            val isHeadsUp =
                                XposedHelpers.callMethod(
                                    expandableView,
                                    "isHeadsUpState"
                                ) as Boolean
                            if (isHeadsUp) {
                                showBlurEffectForNotificationRow(expandableView)
                            }
                        } catch (e: Throwable) {
                            return
                        }
                    }
                }
            })

        // 锁屏状态透明度修改的时候同步修改模糊透明度
        XposedBridge.hookAllMethods(
            MiuiNotificationPanelViewControllerClass,
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
                    mNotificationStackScroller as ViewGroup

                    val keyguardContentsAlpha =
                        XposedHelpers.callMethod(
                            param.thisObject,
                            "getKeyguardContentsAlpha"
                        ) as Float
                    val drawableAlpha = keyguardContentsAlpha * 255
                    for (i in 0..mNotificationStackScroller.childCount) {
                        val childAt =
                            mNotificationStackScroller.getChildAt(i) ?: continue
                        setBlurEffectAlphaForNotificationRow(childAt, drawableAlpha.toInt())
                    }
                }
            })

        XposedBridge.hookAllMethods(
            MiuiNotificationPanelViewControllerClass,
            "onBouncerShowingChanged",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val isBouncerShowing = param.args[0] as Boolean
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    for (i in 0..mNotificationStackScroller.childCount) {
                        val childAt =
                            mNotificationStackScroller.getChildAt(i) ?: continue
                        if (isBouncerShowing) {
                            hideBlurEffectForNotificationRow(childAt)
                        } else {
                            showBlurEffectForNotificationRow(childAt)
                        }
                    }
                }
            })

        // 锁屏画报 隐藏模糊
        XposedBridge.hookAllMethods(
            LockScreenMagazineControllerClass,
            "setViewsAlpha",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
                    val alpha = param.args[0] as Float
                    val drawableAlpha = alpha * 255
                    val mNotificationStackScrollLayout = HookUtils.getValueByField(
                        param.thisObject,
                        "mNotificationStackScrollLayout"
                    ) as ViewGroup
                    for (i in 0..mNotificationStackScrollLayout.childCount) {
                        val childAt =
                            mNotificationStackScrollLayout.getChildAt(i) ?: continue
                        setBlurEffectAlphaForNotificationRow(childAt, drawableAlpha.toInt())
                    }
                }
            })

        XposedBridge.hookAllMethods(
            NotificationStackScrollLayoutClass,
            "setDozing",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val isDozing = param.args[0] as Boolean
                    val mNotificationStackScrollLayout = param.thisObject as ViewGroup
                    for (i in 0..mNotificationStackScrollLayout.childCount) {
                        val childAt =
                            mNotificationStackScrollLayout.getChildAt(i) ?: continue
                        if (isDozing) {
                            hideBlurEffectForNotificationRow(childAt)
                        } else {
                            showBlurEffectForNotificationRow(childAt)
                        }
                    }
                }
            })

        /*
        XposedBridge.hookAllMethods(
            KeyguardPanelViewInjectorClass,
            "onKeyguardVisibilityChanged",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val isVisible = param.args[0] as Boolean
                    val mPanelViewController =
                        Hook.getValueByField(param.thisObject, "mPanelViewController") ?: return

                    val mNotificationStackScroller =
                        Hook.getValueByField(
                            mPanelViewController,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    for (i in 0..mNotificationStackScroller.childCount) {
                        val childAt =
                            mNotificationStackScroller.getChildAt(i) ?: continue
                        if (isVisible) {
                            showBlurEffectForNotificationRow(childAt)
                        } else {
                            try {
                                val isHeadsUp =
                                    XposedHelpers.callMethod(
                                        childAt,
                                        "isHeadsUpState"
                                    ) as Boolean
                                val isPinned = XposedHelpers.callMethod(
                                    childAt,
                                    "isPinned"
                                ) as Boolean
                                if (isHeadsUp && isPinned) {
                                    showBlurEffectForNotificationRow(childAt)
                                } else {
                                    hideBlurEffectForNotificationRow(childAt)
                                }
                            } catch (e: Throwable) {
                                hideBlurEffectForNotificationRow(childAt)
                            }
                        }
                    }
                }
            })
        */

        XposedBridge.hookAllConstructors(MiuiNotificationPanelViewControllerClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val miuiNotificationPanelViewControllerClass = param.thisObject
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    XposedBridge.hookAllMethods(BlurRatioChangedListener,
                        "onBlurRadiusChanged",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val radius = param.args[0] as Int
                                val isOnKeyguard = XposedHelpers.callMethod(
                                    miuiNotificationPanelViewControllerClass,
                                    "isOnKeyguard"
                                ) as Boolean
                                for (i in 0..mNotificationStackScroller.childCount) {
                                    val childAt =
                                        mNotificationStackScroller.getChildAt(i) ?: continue
                                    if (radius > 30) {
                                        hideBlurEffectForNotificationRow(childAt)
                                    } else {
                                        // 锁屏状态显示模糊
                                        if (isOnKeyguard) {
                                            showBlurEffectForNotificationRow(childAt)
                                        }
                                    }
                                }
                            }
                        })
                }
            })
    }

    fun addBlurEffectToLockScreen() {
        val MiuiNotificationPanelViewControllerClass = HookUtils.getClass(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController",
            classLoader
        ) ?: return

        val KeyguardBottomAreaViewClass = HookUtils.getClass(
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView",
            classLoader
        ) ?: return

        val KeyguardMoveHelperClass = HookUtils.getClass(
            "com.android.keyguard.KeyguardMoveHelper",
            classLoader
        ) ?: return

        val BaseKeyguardMoveHelperClass = HookUtils.getClass(
            "com.android.keyguard.BaseKeyguardMoveHelper",
            classLoader
        ) ?: return

        val LockScreenMagazineControllerClass = HookUtils.getClass(
            "com.android.keyguard.magazine.LockScreenMagazineController",
            classLoader
        ) ?: return

        XposedBridge.hookAllMethods(
            KeyguardBottomAreaViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isDefaultLockScreenTheme()) {
                        return
                    }
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
            })

        XposedBridge.hookAllMethods(
            MiuiNotificationPanelViewControllerClass,
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
            MiuiNotificationPanelViewControllerClass,
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

        XposedBridge.hookAllMethods(
            KeyguardMoveHelperClass,
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

        XposedBridge.hookAllMethods(
            BaseKeyguardMoveHelperClass,
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
                    } else {
                        if (!mKeyguardBouncerShowing) {
                            targetBlurView.background = null
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            MiuiNotificationPanelViewControllerClass,
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
                            MiuiNotificationPanelViewControllerClass.getDeclaredField("mKeyguardBouncerFraction")
                        mKeyguardBouncerFractionField.isAccessible = true
                        mKeyguardBouncerFractionField.set(param.thisObject, 1f)
                    }
                }
            })

        XposedBridge.hookAllMethods(
            LockScreenMagazineControllerClass,
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
            KeyguardBottomAreaViewClass,
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

    fun showBlurEffectForNotificationRow(notificationRow: View) {
        if (notificationRow.javaClass.name.contains("ZenModeView")) {
            val zenModeContentContainer =
                XposedHelpers.callMethod(notificationRow, "getContentView") ?: return
            zenModeContentContainer as ViewGroup
            val zenModeContent =
                zenModeContentContainer.getChildAt(0) ?: return
            val contentBackground =
                zenModeContent.background as GradientDrawable
            contentBackground.alpha = blurBackgroundAlpha
            contentBackground.invalidateSelf()
            if (!HookUtils.isBlurDrawable(zenModeContentContainer.background)) {
                zenModeContentContainer.background =
                    HookUtils.createBlurDrawable(notificationRow, blurRadius, cornerRadius)
            }
        } else {
            val mBackgroundNormal =
                HookUtils.getValueByField(notificationRow, "mBackgroundNormal")
                    ?: return
            mBackgroundNormal as View
            if (!HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                mBackgroundNormal.background =
                    HookUtils.createBlurDrawable(mBackgroundNormal, blurRadius, cornerRadius)
                try {
                    XposedHelpers.callMethod(
                        mBackgroundNormal,
                        "setDrawableAlpha",
                        blurBackgroundAlpha
                    )
                } catch (e: Throwable) {
                    // Nothing to do.
                }
            }
            try {
                val childList =
                    XposedHelpers.callMethod(notificationRow, "getAttachedChildren") ?: return
                childList as List<*>
                if (childList.size > 0) {
                    childList.forEach { child ->
                        if (child != null) {
                            showBlurEffectForNotificationRow(child as View)
                        }
                    }
                }
            } catch (e: Throwable) {
                // Nothing to do.
            }
        }
    }

    fun hideBlurEffectForNotificationRow(notificationRow: View) {
        if (notificationRow.javaClass.name.contains("ZenModeView")) {
            val zenModeContentContainer =
                XposedHelpers.callMethod(notificationRow, "getContentView") ?: return
            zenModeContentContainer as ViewGroup
            val zenModeContent =
                zenModeContentContainer.getChildAt(0) ?: return
            val contentBackground =
                zenModeContent.background as GradientDrawable
            contentBackground.alpha = defaultBackgroundAlpha
            contentBackground.invalidateSelf()
            if (HookUtils.isBlurDrawable(zenModeContentContainer.background)) {
                zenModeContentContainer.background = null
            }
        } else {
            val mBackgroundNormal =
                HookUtils.getValueByField(notificationRow, "mBackgroundNormal")
                    ?: return
            mBackgroundNormal as View
            if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                mBackgroundNormal.background = null
            }
            try {
                XposedHelpers.callMethod(
                    mBackgroundNormal,
                    "setDrawableAlpha",
                    defaultBackgroundAlpha
                )
            } catch (e: Throwable) {
                // Nothing to do.
            }

            try {
                val childList =
                    XposedHelpers.callMethod(notificationRow, "getAttachedChildren") ?: return
                childList as List<*>
                if (childList.size > 0) {
                    childList.forEach { child ->
                        if (child != null) {
                            hideBlurEffectForNotificationRow(child as View)
                        }
                    }
                }
            } catch (e: Throwable) {
                // Nothing to do.
            }
        }
    }

    fun setBlurEffectAlphaForNotificationRow(notificationRow: View, alpha: Int) {
        if (alpha < 0 || alpha > 255) {
            return
        }
        if (notificationRow.javaClass.name.contains("ZenModeView")) {
            val zenModeContentContainer =
                XposedHelpers.callMethod(notificationRow, "getContentView") ?: return
            zenModeContentContainer as ViewGroup
            if (HookUtils.isBlurDrawable(zenModeContentContainer.background)) {
                XposedHelpers.callMethod(zenModeContentContainer.background, "setAlpha", alpha)
            }
        } else {
            val mBackgroundNormal =
                HookUtils.getValueByField(notificationRow, "mBackgroundNormal")
                    ?: return
            mBackgroundNormal as View
            if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                XposedHelpers.callMethod(mBackgroundNormal.background, "setAlpha", alpha)
            }
            try {
                val childList =
                    XposedHelpers.callMethod(notificationRow, "getAttachedChildren") ?: return
                childList as List<*>
                if (childList.size > 0) {
                    childList.forEach { child ->
                        if (child != null) {
                            setBlurEffectAlphaForNotificationRow(child as View, alpha)
                        }
                    }
                }
            } catch (e: Throwable) {
                // Nothing to do.
            }
        }
    }

    fun isDefaultLockScreenTheme(): Boolean {
        val MiuiKeyguardUtilsClass = HookUtils.getClass(
            "com.android.keyguard.utils.MiuiKeyguardUtils",
            classLoader
        ) ?: return true
        return XposedHelpers.callStaticMethod(
            MiuiKeyguardUtilsClass,
            "isDefaultLockScreenTheme"
        ) as Boolean
    }

    fun setQSControlDetailBackgroundAlpha() {
        val QSControlDetailClass = HookUtils.getClass(
            "com.android.systemui.controlcenter.phone.detail.QSControlDetail",
            classLoader
        )
        if(QSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                QSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qsControlDetailBackgroundAlpha
                        }
                    }
                })
        }
        val ModalQSControlDetailClass = HookUtils.getClass(
            "com.android.systemui.statusbar.notification.modal.ModalQSControlDetail",
            classLoader
        )
        if(ModalQSControlDetailClass != null){
            XposedHelpers.findAndHookMethod(
                ModalQSControlDetailClass,
                "updateBackground",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mDetailContainer = HookUtils.getValueByField(param.thisObject,"mDetailContainer") as View
                        if(mDetailContainer.background != null){
                            val smoothRoundDrawable = mDetailContainer.background
                            smoothRoundDrawable.alpha = qsControlDetailBackgroundAlpha
                        }
                    }
                })
        }

        hookClassInPlugin{classLoader ->
            try {
                val SmoothRoundDrawableClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "miui.systemui.widget.SmoothRoundDrawable"
                ) ?: return@hookClassInPlugin
                XposedBridge.hookAllMethods(
                    SmoothRoundDrawableClass as Class<*>,
                    "inflate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val currentDrawable = param.thisObject as Drawable
                                currentDrawable.alpha = qsControlDetailBackgroundAlpha
                            } catch (e: Throwable) {
                                // Do Nothings.
                                HookUtils.log(e.message)
                            }
                        }
                    })
            } catch (e: Throwable) {
                HookUtils.log(e.message)
            }
        }
    }

    fun hideControlsPlugin() {
        val MiPlayPluginManagerClass = HookUtils.getClass(
            "com.android.systemui.controlcenter.phone.controls.MiPlayPluginManager",
            classLoader
        )
        if(MiPlayPluginManagerClass != null){
            XposedBridge.hookAllMethods(
                MiPlayPluginManagerClass,
                "supportMiPlayAudio",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })
        }


        hookClassInPlugin{classLoader ->
            try {
                /*
                val MiLinkControllerClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "miui.systemui.util.MiLinkController"
                ) ?: return@hookClassInPlugin
                XposedBridge.hookAllMethods(
                    MiLinkControllerClass as Class<*>,
                    "getMiLinkPackageAvailable",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                param.result = false
                            } catch (e: Throwable) {
                                // Do Nothings.
                                HookUtils.log(e.message)
                            }
                        }
                    })
                */

                val MiPlayControllerClass =
                    XposedHelpers.callMethod(
                        classLoader,
                        "loadClass",
                        "com.android.systemui.MiPlayController"
                    ) ?: return@hookClassInPlugin
                XposedBridge.hookAllMethods(
                    MiPlayControllerClass as Class<*>,
                    "supportMiPlayAudio",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                param.result = false
                            } catch (e: Throwable) {
                                // Do Nothings.
                                HookUtils.log(e.message)
                            }
                        }
                    })
            } catch (e: Throwable) {
                HookUtils.log(e.message)
            }
        }
    }

    fun enableBlurForMTK() {
        hookClassInPlugin{classLoader ->
            try {
                val VolumeUtilClass = XposedHelpers.callMethod(
                    classLoader,
                    "loadClass",
                    "com.android.systemui.miui.volume.Util"
                ) ?: return@hookClassInPlugin
                VolumeUtilClass as Class<*>
                val allVolumeUtilMethods = VolumeUtilClass.methods
                if (allVolumeUtilMethods.isEmpty()) {
                    return@hookClassInPlugin
                }
                allVolumeUtilMethods.forEach { method ->
                    if (method.name == "isSupportBlurS") {
                        XposedBridge.hookAllMethods(
                            VolumeUtilClass,
                            "isSupportBlurS",
                            object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        if (param.result is Boolean) {
                                            param.result = true
                                        }
                                    } catch (e: Throwable) {
                                        // Do Nothings.
                                        HookUtils.log(e.message)
                                    }
                                }
                            })
                        return@hookClassInPlugin
                    }
                }
            }catch (e: Throwable) {
                // Do Nothings.
                HookUtils.log(e.message)
            }
        }
    }

    fun hookClassInPlugin(afterGetClassLoader: (classLoader: ClassLoader) -> Unit){
        val PluginHandlerClass = HookUtils.getClass(
            "com.android.systemui.shared.plugins.PluginInstanceManager\$PluginHandler",
            classLoader
        )
        if (PluginHandlerClass != null) {
            XposedBridge.hookAllMethods(
                PluginHandlerClass,
                "handleLoadPlugin",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val componentName = param.args[0]
                        val className =
                            XposedHelpers.callMethod(componentName, "getClassName") as String
                        if (className != "miui.systemui.volume.VolumeDialogPlugin") {
                            return
                        }
                        try {
                            val pluginContextWrapper =
                                HookUtils.getValueByField(param.result ?: return, "mPluginContext") ?: return
                            val classLoader = XposedHelpers.callMethod(
                                pluginContextWrapper,
                                "getClassLoader"
                            ) as ClassLoader
                            afterGetClassLoader(classLoader)
                        } catch (e: Throwable) {
                            // Do Nothings.
                            HookUtils.log(e.message)
                        }
                    }
                })
            return
        }

        val PluginActionManagerClass = HookUtils.getClass(
            "com.android.systemui.shared.plugins.PluginActionManager",
            classLoader
        )
        if (PluginActionManagerClass != null) {
            XposedBridge.hookAllMethods(
                PluginActionManagerClass,
                "loadPluginComponent",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val componentName = param.args[0]
                        val className =
                            XposedHelpers.callMethod(componentName, "getClassName") as String
                        if (className != "miui.systemui.volume.VolumeDialogPlugin") {
                            return
                        }
                        try {
                            val pluginContextWrapper =
                                HookUtils.getValueByField(param.result ?: return, "mPluginContext")
                                    ?: return
                            val classLoader = XposedHelpers.callMethod(
                                pluginContextWrapper,
                                "getClassLoader"
                            ) as ClassLoader
                            afterGetClassLoader(classLoader)
                        } catch (e: Throwable) {
                            // Do Nothings.
                            HookUtils.log(e.message)
                        }
                    }
                })
            return
        }
    }

}