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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.blur.BlurUtils.createBlurDrawable
import com.sevtinge.hyperceiler.utils.blur.BlurUtils.isBlurDrawable
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

@SuppressLint("NewApi")
object AddBlurEffectToNotificationView : BaseHook() {
    var blurBackgroundAlpha: Int =
        mPrefsMap.getInt("system_ui_control_center_blur_background_alpha", 100)
    var cornerRadius: Int = mPrefsMap.getInt("system_ui_control_center_corner_radius", 48)
    var blurRadius: Int = mPrefsMap.getInt("system_ui_control_center_blur_radius", 99)
    var defaultBackgroundAlpha: Int =
        mPrefsMap.getInt("system_ui_control_center_default_background_alpha", 200)

    fun setDrawableAlpha(thiz: Any?, alpha: Int) {
        if (isAndroidVersion(34)) {
            XposedHelpers.setObjectField(thiz, "mDrawableAlpha", alpha)
        } else {
            XposedHelpers.callMethod(
                thiz,
                "setDrawableAlpha",
                arrayOf<Class<*>>(Integer.TYPE),
                alpha
            )
        }
    }

    override fun init() {
        val miuiExpandableNotificationRowClass =
            findClassIfExists("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow")
                ?: return

        val notificationBackgroundViewClass =
            findClassIfExists("com.android.systemui.statusbar.notification.row.NotificationBackgroundView")
                ?: return

        val appMiniWindowRowTouchHelperClass =
            findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowRowTouchHelper")
                ?: return

        val miuiNotificationPanelViewControllerClass =
            findClassIfExists(
                if (isAndroidVersion(34))
                    "com.android.systemui.shade.MiuiNotificationPanelViewController"
                else
                    "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController"
            ) ?: return

        val notificationStackScrollLayoutClass =
            findClassIfExists("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout")
                ?: return

        val lockScreenMagazineControllerClass =
            findClassIfExists("com.android.keyguard.magazine.LockScreenMagazineController")
                ?: return

        val blurRatioChangedListener =
            findClassIfExists(
                if (isAndroidVersion(34))
                    "com.android.systemui.shade.MiuiNotificationPanelViewController\$mBlurRatioChangedListener\$1"
                else
                    "com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController\$mBlurRatioChangedListener\$1"
            ) ?: return

        // 通知模糊额外修正项，增加一个开关避免使用过程中暴毙
        if (isAndroidVersion(33) && mPrefsMap.getBoolean("n_enable_fix")) {
            val mediaDataFilterClass =
                findClassIfExists("com.android.systemui.media.MediaDataFilter") ?: return

            val expandableNotificationRowClass =
                findClassIfExists("com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")
                    ?: return

            // 增加一个锁屏页面判断
            var onKeyguard = false

            expandableNotificationRowClass.hookAfterMethod("isOnKeyguard") {
                onKeyguard = it.result as Boolean
            }

            // 增加一个控制中心音乐播放器判断
            var hasActiveMediaOrRecommendation = false

            mediaDataFilterClass.hookAfterMethod("hasActiveMediaOrRecommendation") {
                hasActiveMediaOrRecommendation = it.result as Boolean
            }


            // 换个方式修改通知上划极限值
            try {
                "com.android.systemui.statusbar.notification.stack.AmbientState".replaceMethod("getOverExpansion") {
                    val getScreenHeight =
                        loadClass("com.android.systemui.fsgesture.AppQuickSwitchActivity")
                            .callStaticMethod("getScreenHeight", appContext) as Int
                    val its = it.thisObject.getObjectField("mOverExpansion")
                    if (its != null) {
                        val mOverExpansion = it.thisObject.getObjectField("mOverExpansion") as Float
                        val isNCSwitching = it.thisObject.getObjectField("isNCSwitching") as Boolean
                        val isSwipingUp = it.thisObject.getObjectField("mIsSwipingUp") as Boolean
                        val isFlinging = it.thisObject.getObjectField("mIsFlinging") as Boolean
                        val isAppearing = it.thisObject.getObjectField("mAppearing") as Boolean
                        val isScreenLandscape =
                            loadClass("com.android.systemui.statusbar.notification.NotificationUtil")
                                .callStaticMethod("isScreenLandscape") as Boolean

                        if (isAppearing && (isSwipingUp || isFlinging) && !isNCSwitching) {
                            if (hasActiveMediaOrRecommendation) {
                                if (isScreenLandscape)
                                    return@replaceMethod -getScreenHeight.toFloat()
                                else
                                    return@replaceMethod -getScreenHeight.toFloat() * 6.0f
                            } else {
                                if (isScreenLandscape)
                                    return@replaceMethod -getScreenHeight.toFloat() / 3.0f
                                else
                                    return@replaceMethod -getScreenHeight.toFloat() / 1.2f
                            }
                        } else {
                            return@replaceMethod mOverExpansion
                        }
                    } else {
                        return@replaceMethod it.result
                    }
                }
            } catch (t: Throwable) {
                logE(TAG, this.lpparam.packageName, t)
            }
            try {
                "com.android.systemui.statusbar.notification.stack.AmbientState".replaceMethod("getAppearFraction") {
                    val isNCSwitching = it.thisObject.getObjectField("isNCSwitching") as Boolean
                    val isSwipingUp = it.thisObject.getObjectField("mIsSwipingUp") as Boolean
                    val isFlinging = it.thisObject.getObjectField("mIsFlinging") as Boolean
                    val mAppearFraction = it.thisObject.getObjectField("mAppearFraction") as Float
                    val isAppearing = it.thisObject.getObjectField("mAppearing") as Boolean
                    val isScreenLandscape =
                        loadClass("com.android.systemui.statusbar.notification.NotificationUtil")
                            .callStaticMethod("isScreenLandscape") as Boolean

                    if (isAppearing && (isSwipingUp || isFlinging) && !isNCSwitching && hasActiveMediaOrRecommendation && isScreenLandscape) {
                        return@replaceMethod mAppearFraction * 6.0f
                    } else {
                        return@replaceMethod mAppearFraction
                    }
                }
            } catch (t: Throwable) {
                logE(TAG, this.lpparam.packageName, t)
            }
        }

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
                        setDrawableAlpha(
                            notificationBackgroundView,
                            blurBackgroundAlpha
                        )
                    } else {
                        mDrawableAlphaField.set(notificationBackgroundView, defaultBackgroundAlpha)
                        setDrawableAlpha(
                            notificationBackgroundView,
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
                    if (isBlurDrawable(backgroundDrawable)) {
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
                        getValueByField(param.thisObject, "mPickedMiniWindowChild")
                            ?: return

                    val mBackgroundNormal =
                        getValueByField(mPickedMiniWindowChild, "mBackgroundNormal")
                            ?: return
                    mBackgroundNormal as View

                    if (isBlurDrawable(mBackgroundNormal.background)) {
                        XposedHelpers.callMethod(
                            mBackgroundNormal.background, "setVisible",
                            false, false
                        )
                        setDrawableAlpha(
                            mBackgroundNormal,
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
                        getValueByField(param.thisObject, "mPickedMiniWindowChild")
                            ?: return

                    val mBackgroundNormal =
                        getValueByField(mPickedMiniWindowChild, "mBackgroundNormal")
                            ?: return
                    mBackgroundNormal as View

                    if (isBlurDrawable(mBackgroundNormal.background)) {
                        XposedHelpers.callMethod(
                            mBackgroundNormal.background,
                            "setVisible",
                            true,
                            false
                        )
                        setDrawableAlpha(
                            mBackgroundNormal,
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
                @SuppressLint("NewApi")
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val isHeadsUp = param.args[0] as Boolean
                    val miuiNotificationBackgroundView = param.thisObject as View
                    val mBackgroundNormal = getValueByField(
                        miuiNotificationBackgroundView,
                        "mBackgroundNormal"
                    ) as View
                    if (!mBackgroundNormal.isAttachedToWindow) {
                        return
                    }
                    if (isHeadsUp) {
                        if (mBackgroundNormal.background != null) {
                            if (isBlurDrawable(mBackgroundNormal.background)) {
                                return
                            }
                        }
                        mBackgroundNormal.background =
                            createBlurDrawable(
                                mBackgroundNormal,
                                blurRadius,
                                cornerRadius
                            )

                        setDrawableAlpha(
                            mBackgroundNormal,
                            blurBackgroundAlpha
                        )
                    } /*else {
                        if (isBlurDrawable(mBackgroundNormal.background)) {
                            mBackgroundNormal.background = null
                        }
                        try {
                            XposedHelpers.callMethod(
                                mBackgroundNormal,
                                "setDrawableAlpha",
                                defaultBackgroundAlpha
                            )
                        } catch (e: Throwable) {
                            logE("BlurNotificationView -> defaultBackgroundAlpha", e)
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
                        getValueByField(
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
                    val mController = getValueByField(param.thisObject, "mController")
                        ?: return
                    val mPanelViewController =
                        getValueByField(mController, "mPanelViewController")
                            ?: return
                    val isExpanding =
                        XposedHelpers.callMethod(
                            mPanelViewController,
                            if (isAndroidVersion(34)) "isExpandingOrCollapsing" else "isExpanding"
                        ) as Boolean
                    if (isExpanding) return

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

        XposedBridge.hookAllMethods(
            miuiNotificationPanelViewControllerClass,
            "onBouncerShowingChanged",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val isBouncerShowing = param.args[0] as Boolean
                    val mNotificationStackScroller =
                        getValueByField(param.thisObject, "mNotificationStackScroller")
                            ?: return
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
            // 修复 Android 13 锁屏画报模糊残留
            XposedBridge.hookAllMethods(
                lockScreenMagazineControllerClass,
                "setPanelViewAlpha",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!isDefaultLockScreenTheme()) return
                        val alpha = param.args[0] as Float
                        val drawableAlpha = alpha * 255

                        val mNotificationStackScrollLayoutController =
                            getValueByField(
                                param.thisObject,
                                "mNotificationStackScrollLayoutController"
                            )
                                ?: return
                        val mView =
                            getValueByField(
                                mNotificationStackScrollLayoutController,
                                "mView"
                            ) ?: return

                        mView as ViewGroup

                        for (i in 0..mView.childCount) {
                            val childAt = mView.getChildAt(i) ?: continue
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
                        getValueByField(
                            param.thisObject,
                            "mNotificationStackScroller"
                        ) ?: return
                    mNotificationStackScroller as ViewGroup
                    XposedBridge.hookAllMethods(blurRatioChangedListener,
                        "onBlurRadiusChanged",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(params: MethodHookParam) {
                                val radius = params.args[0] as Int
                                val isOnKeyguard = XposedHelpers.callMethod(
                                    param.thisObject,
                                    "isOnKeyguard"
                                ) as Boolean
                                for (i in 0..mNotificationStackScroller.childCount) {
                                    val childAt =
                                        mNotificationStackScroller.getChildAt(i) ?: continue
                                    if (radius > 30 && !isOnKeyguard) {
                                        hideBlurEffectForNotificationRow(childAt)
                                    } /*else {
                                        // 锁屏状态显示模糊（不能留，点击通知进入密码页面模糊残留）
                                        if (isOnKeyguard) showBlurEffectForNotificationRow(childAt)
                                    }*/
                                }
                            }
                        })
                }
            })
    }

    fun isDefaultLockScreenTheme(): Boolean {
        val miuiKeyguardUtilsClass = findClassIfExists(
            if (isAndroidVersion(34))
                "com.miui.systemui.util.CommonUtil"
            else
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
            if (isBlurDrawable(zenModeContentContainer.background)) {
                zenModeContentContainer.background = null
            }
        } else {
            val mBackgroundNormal =
                getValueByField(notificationRow, "mBackgroundNormal")
                    ?: return
            mBackgroundNormal as View
            if (isBlurDrawable(mBackgroundNormal.background)) {
                mBackgroundNormal.background = null
            }
            runCatching {
                setDrawableAlpha(
                    mBackgroundNormal,
                    defaultBackgroundAlpha
                )
            }

            runCatching {
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
            if (isBlurDrawable(zenModeContentContainer.background)) {
                XposedHelpers.callMethod(zenModeContentContainer.background, "setAlpha", alpha)
            }
        } else {
            val mBackgroundNormal =
                getValueByField(notificationRow, "mBackgroundNormal")
                    ?: return
            mBackgroundNormal as View
            if (isBlurDrawable(mBackgroundNormal.background)) {
                XposedHelpers.callMethod(mBackgroundNormal.background, "setAlpha", alpha)
            }
            runCatching {
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
            }
        }
    }

    @SuppressLint("NewApi")
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
            if (!isBlurDrawable(zenModeContentContainer.background)) {
                zenModeContentContainer.background =
                    createBlurDrawable(notificationRow, blurRadius, cornerRadius)
            }
        } else {
            val mBackgroundNormal =
                getValueByField(notificationRow, "mBackgroundNormal") ?: return
            mBackgroundNormal as View
            if (!isBlurDrawable(mBackgroundNormal.background)) {
                mBackgroundNormal.background =
                    createBlurDrawable(mBackgroundNormal, blurRadius, cornerRadius)
                runCatching {
                    setDrawableAlpha(
                        mBackgroundNormal,
                        blurBackgroundAlpha
                    )
                }
            }
            runCatching {
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
            }
        }
    }
}
