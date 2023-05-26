package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import com.sevtinge.cemiuiler.utils.XSPUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object AddBlurEffectToNotificationView : BaseHook() {

    var blurBackgroundAlpha: Int = mPrefsMap.getInt("system_ui_control_center_blur_background_alpha", 100)
    var cornerRadius: Int = mPrefsMap.getInt("system_ui_control_center_corner_radius", 48)
    var blurRadius: Int = mPrefsMap.getInt("system_ui_control_center_blur_radius", 99)
    var defaultBackgroundAlpha: Int = XSPUtils.getInt("system_ui_control_center_default_background_alpha", 200)


    override fun init() {
        val miuiExpandableNotificationRowClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow"
        ) ?: return

        val notificationBackgroundViewClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.row.NotificationBackgroundView"
        ) ?: return

        val appMiniWindowRowTouchHelperClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.policy.AppMiniWindowRowTouchHelper"
        ) ?: return

        val miuiNotificationPanelViewControllerClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController"
        ) ?: return

        val notificationStackScrollLayoutClass = findClassIfExists(
            "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout"
        ) ?: return

        val lockScreenMagazineControllerClass = findClassIfExists(
            "com.android.keyguard.magazine.LockScreenMagazineController"
        ) ?: return

        val blurRatioChangedListener = findClassIfExists(
            "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController\$mBlurRatioChangedListener\$1"
        ) ?: return


        // 每次设置背景的时候都同时改透明度
        XposedBridge.hookAllMethods(
            notificationBackgroundViewClass,
            "setCustomBackground",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val notificationBackgroundView = param.thisObject
                    val mDrawableAlphaField =
                        notificationBackgroundViewClass.getDeclaredField("mDrawableAlpha")
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
            notificationBackgroundViewClass,
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
            appMiniWindowRowTouchHelperClass,
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
            appMiniWindowRowTouchHelperClass,
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
            miuiExpandableNotificationRowClass,
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
                        return
                    }
                    if (isHeadsUp) {
                        if (mBackgroundNormal.background != null) {
                            if (HookUtils.isBlurDrawable(mBackgroundNormal.background)) {
                                return
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
                    } /*else {
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
                    }*/
                }
            })

        // 进入不同状态，处理一下模糊
        XposedBridge.hookAllMethods(
            miuiNotificationPanelViewControllerClass,
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
            notificationStackScrollLayoutClass,
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
            miuiNotificationPanelViewControllerClass,
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
            lockScreenMagazineControllerClass,
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
            notificationStackScrollLayoutClass,
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

        XposedBridge.hookAllConstructors(miuiNotificationPanelViewControllerClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mNotificationStackScroller =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    val mNotificationPanelViewController =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mNotificationPanelViewController"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    XposedBridge.hookAllMethods(blurRatioChangedListener,
                        "onBlurRadiusChanged",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val radius = param.args[0] as Int
                                val isOnKeyguard = XposedHelpers.callMethod(
                                    mNotificationPanelViewController,
                                    "isOnKeyguard"
                                ) as Boolean
                                for (i in 0..mNotificationStackScroller.childCount) {
                                    val childAt =
                                        mNotificationStackScroller.getChildAt(i) ?: continue
                                    if (radius > 30 && !isOnKeyguard) {
                                        hideBlurEffectForNotificationRow(childAt)
                                    } /*else {
                                        // 锁屏状态显示模糊
                                        if (isOnKeyguard) {
                                            showBlurEffectForNotificationRow(childAt)
                                        }
                                    }*/
                                }
                            }
                        })
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
}
